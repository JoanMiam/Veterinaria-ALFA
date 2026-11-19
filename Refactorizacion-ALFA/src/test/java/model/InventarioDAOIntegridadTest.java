package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import prueba.BdPrueba;

/**
 * PRU-009-01 — Integridad de datos (FIX-009): transacción en editarVenta, devolución de
 * stock en eliminarVenta, borrado lógico, migración y búsqueda por nombre/lote (FEFO).
 *
 * <p>[MR-010] Adaptado a la API tipada; los rechazos se verifican con
 * {@link OperacionRechazadaException} en lugar de capturar {@code HeadlessException}.
 */
@DisplayName("PRU-009-01 | InventarioDAO — Integridad de datos (FIX-009)")
class InventarioDAOIntegridadTest {

    private static final LocalDate HOY = LocalDate.now();

    private BdPrueba bd;
    private InventarioDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        bd = BdPrueba.nueva();
        dao = bd.dao;
    }

    @AfterEach
    void tearDown() throws SQLException {
        bd.close();
    }

    private int venderYObtenerId(int idProducto, int cantidad) throws OperacionRechazadaException {
        return dao.registrarVenta(idProducto, cantidad).id();
    }

    // =========================================================================
    // editarVenta: transacción
    // =========================================================================

    @Test
    @DisplayName("TC-01: editarVenta al aumentar la cantidad descuenta la diferencia del stock")
    void tc01_editarVenta_aumentaCantidad_ajustaStock() throws Exception {
        int id = bd.insertarProducto("Amoxicilina", 50, BdPrueba.vigente());
        int venta = venderYObtenerId(id, 5);

        dao.editarVenta(venta, 8, HOY);

        assertEquals(42, bd.existencias(id), "50 - 8 = 42");
        assertEquals(8, bd.cantidadVenta(venta));
    }

    @Test
    @DisplayName("TC-02: si falla la actualización de la venta, el ajuste de stock se deshace")
    void tc02_editarVenta_falloEnVenta_haceRollbackDelStock() throws Exception {
        int id = bd.insertarProducto("Amoxicilina", 50, BdPrueba.vigente());
        int venta = venderYObtenerId(id, 5);
        bd.ejecutar("CREATE TRIGGER falla_venta BEFORE UPDATE ON historial_ventas "
                + "BEGIN SELECT RAISE(ABORT, 'falla simulada'); END");

        assertThrows(ErrorDatosException.class, () -> dao.editarVenta(venta, 20, HOY));

        assertEquals(45, bd.existencias(id), "El stock queda como antes de la edición (50 - 5)");
        assertEquals(5, bd.cantidadVenta(venta), "La venta no cambia");
    }

    @Test
    @DisplayName("TC-03: editarVenta rechaza cantidad 0 sin tocar stock ni venta")
    void tc03_editarVenta_cantidadCero_seRechaza() throws Exception {
        int id = bd.insertarProducto("Amoxicilina", 50, BdPrueba.vigente());
        int venta = venderYObtenerId(id, 5);

        assertThrows(OperacionRechazadaException.class, () -> dao.editarVenta(venta, 0, HOY));

        assertEquals(45, bd.existencias(id));
        assertEquals(5, bd.cantidadVenta(venta));
    }

    @Test
    @DisplayName("TC-04: editarVenta rechaza un aumento mayor al stock disponible")
    void tc04_editarVenta_aumentoSinStock_seRechaza() throws Exception {
        int id = bd.insertarProducto("Amoxicilina", 10, BdPrueba.vigente());
        int venta = venderYObtenerId(id, 5);

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> dao.editarVenta(venta, 16, HOY));

        assertTrue(e.getMessage().contains("existencias"), e.getMessage());
        assertEquals(5, bd.existencias(id));
        assertEquals(5, bd.cantidadVenta(venta));
    }

    // =========================================================================
    // eliminarVenta: regresa stock
    // =========================================================================

    @Test
    @DisplayName("TC-05: eliminarVenta regresa la cantidad al stock y borra el registro")
    void tc05_eliminarVenta_regresaStock() throws Exception {
        int id = bd.insertarProducto("Paracetamol", 30, BdPrueba.vigente());
        int venta = venderYObtenerId(id, 7);
        assertEquals(23, bd.existencias(id));

        dao.eliminarVenta(venta);

        assertEquals(30, bd.existencias(id));
        assertEquals(0, bd.contar("historial_ventas"));
    }

    @Test
    @DisplayName("TC-06: eliminarVenta con id inexistente se rechaza sin cambios")
    void tc06_eliminarVenta_idInexistente() throws Exception {
        int id = bd.insertarProducto("Paracetamol", 30, BdPrueba.vigente());

        assertThrows(OperacionRechazadaException.class, () -> dao.eliminarVenta(9999));

        assertEquals(30, bd.existencias(id));
    }

    @Test
    @DisplayName("TC-07: eliminarVenta de una venta huérfana (producto borrado en versiones previas) solo borra la venta")
    void tc07_eliminarVenta_huerfana() throws Exception {
        int venta = bd.insertarVenta(777, "Viejo", 3, "2025-01-01");

        dao.eliminarVenta(venta);

        assertEquals(0, bd.contar("historial_ventas"));
    }

    // =========================================================================
    // eliminarProducto: borrado lógico
    // =========================================================================

    @Test
    @DisplayName("TC-08: eliminarProducto oculta el producto pero conserva su historial editable")
    void tc08_eliminarProducto_ocultaYConservaHistorial() throws Exception {
        int id = bd.insertarProducto("Ketoprofeno", 20, BdPrueba.vigente());
        int venta = venderYObtenerId(id, 4);

        dao.eliminarProducto(id);

        assertTrue(dao.obtenerProductos().isEmpty(), "No aparece en inventario");
        assertTrue(dao.buscarProductoPorIdONombre("Ketoprofeno").isEmpty(), "No se encuentra por nombre");
        assertTrue(dao.buscarProductoPorIdONombre(String.valueOf(id)).isEmpty(), "No se encuentra por ID");
        assertEquals(1, dao.obtenerHistorialVentas().size(), "La venta sigue en el historial");
        dao.editarVenta(venta, 2, HOY); // la venta de un producto oculto se sigue editando
        assertThrows(OperacionRechazadaException.class, () -> dao.eliminarProducto(id),
                "Eliminar dos veces se rechaza");
    }

    @Test
    @DisplayName("TC-09: no se puede vender un producto eliminado")
    void tc09_registrarVenta_productoEliminado() throws Exception {
        int id = bd.insertarProducto("Ketoprofeno", 20, BdPrueba.vigente());
        dao.eliminarProducto(id);

        assertThrows(OperacionRechazadaException.class, () -> dao.registrarVenta(id, 1));

        assertEquals(20, bd.existencias(id));
        assertEquals(0, bd.contar("historial_ventas"));
    }

    // =========================================================================
    // Migración de esquema
    // =========================================================================

    @Test
    @DisplayName("TC-10: una BD con el esquema anterior recibe la columna 'activo' sin perder datos")
    void tc10_migracion_agregaColumnaActivo() throws Exception {
        try (BdPrueba vieja = BdPrueba.conEsquemaAnterior("Meloxicam", 12, BdPrueba.vigente())) {
            assertEquals(1, vieja.dao.obtenerProductos().size(), "El producto existente sigue visible");
            assertEquals("Meloxicam", vieja.dao.obtenerProductos().get(0).nombre());
            assertEquals(1, vieja.entero("SELECT activo FROM productos"), "Los productos existentes quedan activos");
        }
    }

    // =========================================================================
    // Búsqueda por nombre y lote
    // =========================================================================

    @Test
    @DisplayName("TC-11: la búsqueda por nombre ignora mayúsculas y espacios, incluso con acentos")
    void tc11_busqueda_ignoraMayusculas() throws Exception {
        int id = bd.insertarProducto("Ácido Tranexámico", 10, BdPrueba.vigente());

        Producto p = dao.buscarProductoPorIdONombre("  ÁCIDO tranexámico ").orElseThrow();

        assertEquals(id, p.id());
        assertEquals("Ácido Tranexámico", p.nombre(), "Devuelve el nombre real registrado");
    }

    @Test
    @DisplayName("TC-12: con varios lotes elige el vendible que caduca primero (FEFO)")
    void tc12_busqueda_variosLotes_eligeFefo() throws Exception {
        bd.insertarProducto("Amoxicilina", 10, BdPrueba.mes(-2)); // caducado
        bd.insertarProducto("Amoxicilina", 0, BdPrueba.mes(1));   // sin stock
        bd.insertarProducto("Amoxicilina", 5, BdPrueba.mes(9));   // vigente, lejano
        int esperado = bd.insertarProducto("Amoxicilina", 5, BdPrueba.mes(3)); // vigente, próximo

        Producto p = dao.buscarProductoPorIdONombre("amoxicilina").orElseThrow();

        assertEquals(esperado, p.id());
        dao.registrarVenta(p.id(), 2); // la venta con ese lote procede
        assertEquals(3, bd.existencias(esperado));
    }

    @Test
    @DisplayName("TC-13: si ningún lote es vendible devuelve el primero (para que la venta explique el motivo)")
    void tc13_busqueda_ningunVendible_devuelvePrimero() throws Exception {
        int primero = bd.insertarProducto("Amoxicilina", 10, BdPrueba.mes(-2));
        bd.insertarProducto("Amoxicilina", 10, BdPrueba.mes(-1));

        assertEquals(primero, dao.buscarProductoPorIdONombre("Amoxicilina").orElseThrow().id());
    }

    // =========================================================================
    // Validaciones de entrada (MR-010: antes las hacía solo la vista)
    // =========================================================================

    @Test
    @DisplayName("TC-14: agregar/editar producto rechazan nombre o lote vacíos y existencias negativas")
    void tc14_validacionesDeProducto() throws Exception {
        assertThrows(OperacionRechazadaException.class,
                () -> dao.agregarProducto(" ", 1, "L1", java.time.YearMonth.now()));
        assertThrows(OperacionRechazadaException.class,
                () -> dao.agregarProducto("Med", 1, "", java.time.YearMonth.now()));
        assertThrows(OperacionRechazadaException.class,
                () -> dao.agregarProducto("Med", -1, "L1", java.time.YearMonth.now()));
        int id = bd.insertarProducto("Med", 5, BdPrueba.vigente());
        assertThrows(OperacionRechazadaException.class,
                () -> dao.editarProducto(id, "Med", 5, "L1", java.time.YearMonth.now(), null));
        assertEquals(1, bd.contar("productos"));
    }

    @Test
    @DisplayName("TC-15: registrarVenta rechaza cantidades no positivas y mayores al stock")
    void tc15_registrarVenta_cantidadesInvalidas() throws Exception {
        int id = bd.insertarProducto("Med", 5, BdPrueba.vigente());

        assertThrows(OperacionRechazadaException.class, () -> dao.registrarVenta(id, 0));
        assertThrows(OperacionRechazadaException.class, () -> dao.registrarVenta(id, -3));
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> dao.registrarVenta(id, 6));

        assertTrue(e.getMessage().contains("disponibles: 5"), e.getMessage());
        assertFalse(bd.contar("historial_ventas") > 0);
        assertEquals(5, bd.existencias(id));
    }
}
