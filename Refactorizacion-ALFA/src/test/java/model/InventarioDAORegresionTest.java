package model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import prueba.BdPrueba;

/**
 * PRU-002-03 — Regresión de las operaciones pre-existentes del DAO (MR-002).
 *
 * <ul>
 *   <li>CR-01 a CR-03: {@code registrarVenta} — flujo de éxito y persistencia en BD.</li>
 *   <li>CR-04 a CR-05: {@code obtenerProductos} y {@code obtenerHistorialVentas}.</li>
 *   <li>CR-06 a CR-07: {@code agregarProducto} y {@code eliminarProducto}.</li>
 *   <li>CR-08: búsqueda por nombre (antes {@code obtenerProductoIdPorNombre}, unificada en MR-010).</li>
 *   <li>CR-09: búsqueda + venta coexisten.</li>
 * </ul>
 */
@DisplayName("PRU-002-03 | Regresión — Operaciones pre-existentes en InventarioDAO (MR-002)")
class InventarioDAORegresionTest {

    private BdPrueba bd;
    private InventarioDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        bd = BdPrueba.nueva();
        dao = bd.dao;
        bd.insertarProducto("Amoxicilina", 50, "L001", BdPrueba.vigente()); // id 1
    }

    @AfterEach
    void tearDown() throws SQLException {
        bd.close();
    }

    @Test
    @DisplayName("CR-01: registrarVenta(id, cantidad) procede en el flujo de éxito")
    void cr01_registrarVenta_procede() {
        Venta v = assertDoesNotThrow(() -> dao.registrarVenta(1, 5));

        assertEquals(5, v.cantidad());
        assertEquals("Amoxicilina", v.nombre());
    }

    @Test
    @DisplayName("CR-02: registrarVenta descuenta correctamente las existencias del producto")
    void cr02_registrarVenta_decrementaExistencias() throws Exception {
        dao.registrarVenta(1, 10);

        assertEquals(40, bd.existencias(1), "50 - 10 = 40");
    }

    @Test
    @DisplayName("CR-03: registrarVenta inserta un registro en historial_ventas")
    void cr03_registrarVenta_insertaEnHistorial() throws Exception {
        dao.registrarVenta(1, 3);

        assertEquals(1, bd.contar("historial_ventas"));
    }

    @Test
    @DisplayName("CR-04: obtenerProductos retorna la lista de productos")
    void cr04_obtenerProductos() {
        List<Producto> productos = dao.obtenerProductos();

        assertEquals(1, productos.size());
        assertEquals("Amoxicilina", productos.get(0).nombre());
    }

    @Test
    @DisplayName("CR-05: obtenerHistorialVentas retorna lista vacía cuando no hay ventas")
    void cr05_obtenerHistorialVentas_listaVaciaInicial() {
        assertTrue(dao.obtenerHistorialVentas().isEmpty());
    }

    @Test
    @DisplayName("CR-06: agregarProducto inserta correctamente un nuevo producto")
    void cr06_agregarProducto_incrementaConteo() throws Exception {
        Producto creado = dao.agregarProducto("Ibuprofeno", 20, "L002", YearMonth.now().plusMonths(8));

        assertEquals(2, bd.contar("productos"));
        assertEquals(creado, dao.buscarProductoPorIdONombre(String.valueOf(creado.id())).orElseThrow());
    }

    @Test
    @DisplayName("CR-07: eliminarProducto oculta el producto (borrado lógico, FIX-009)")
    void cr07_eliminarProducto_borradoLogico() throws Exception {
        dao.eliminarProducto(1);

        assertTrue(dao.obtenerProductos().isEmpty(), "No debe aparecer en el inventario");
        assertEquals(1, bd.contar("productos"), "La fila se conserva (activo = 0) para no perder el historial");
    }

    @Test
    @DisplayName("CR-08: la búsqueda por nombre retorna el id correcto")
    void cr08_busquedaPorNombre_retornaIdCorrecto() {
        assertEquals(1, dao.buscarProductoPorIdONombre("Amoxicilina").orElseThrow().id());
    }

    @Test
    @DisplayName("CR-09: buscarProductoPorIdONombre y registrarVenta coexisten sin interferencia")
    void cr09_busquedaYRegistro_coexisten() throws Exception {
        Producto producto = dao.buscarProductoPorIdONombre("Amoxicilina").orElseThrow();

        dao.registrarVenta(producto.id(), 5);

        assertEquals(45, bd.existencias(1), "50 - 5 = 45");
    }
}
