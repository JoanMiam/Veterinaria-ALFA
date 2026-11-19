package controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.OperacionRechazadaException;
import model.Venta;
import prueba.BdPrueba;

/**
 * PRU-010-02 — InventarioController como capa de servicio (MR-010): avisos de cambio,
 * venta/apartado por ID o nombre y operaciones por lote.
 */
@DisplayName("PRU-010-02 | InventarioController — servicio y avisos de cambio (MR-010)")
class InventarioControllerTest {

    private BdPrueba bd;
    private InventarioController controller;
    private final AtomicInteger avisos = new AtomicInteger();

    @BeforeEach
    void setUp() throws SQLException {
        bd = BdPrueba.nueva();
        controller = new InventarioController(bd.dao);
        controller.alCambiarInventario(avisos::incrementAndGet);
    }

    @AfterEach
    void tearDown() throws SQLException {
        bd.close();
    }

    @Test
    @DisplayName("TC-01: las operaciones que cambian el inventario avisan a los suscriptores")
    void tc01_cambiosAvisan() throws Exception {
        controller.agregarProducto("Amoxicilina", 10, "L1", YearMonth.now().plusMonths(6));
        Venta venta = controller.registrarVenta("Amoxicilina", 2);
        controller.editarVenta(venta.id(), 3, LocalDate.now());
        controller.apartarProducto("Amoxicilina", LocalDate.now());
        controller.eliminarVentas(List.of(venta.id()));

        assertEquals(5, avisos.get());
    }

    @Test
    @DisplayName("TC-02: las consultas y los rechazos no avisan")
    void tc02_consultasYRechazosNoAvisan() {
        controller.obtenerProductos();
        controller.obtenerHistorialVentas();
        assertThrows(OperacionRechazadaException.class, () -> controller.registrarVenta("NoExiste", 1));

        assertEquals(0, avisos.get());
    }

    @Test
    @DisplayName("TC-03: registrarVenta por nombre inexistente explica el motivo")
    void tc03_ventaPorNombreInexistente() {
        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> controller.registrarVenta("Fantasma", 1));

        assertTrue(e.getMessage().contains("No se encontró"), e.getMessage());
    }

    @Test
    @DisplayName("TC-04: registrarVenta por nombre usa el lote FEFO y devuelve la venta")
    void tc04_ventaPorNombreUsaFefo() throws Exception {
        bd.insertarProducto("Amoxicilina", 5, BdPrueba.mes(9));
        int proximo = bd.insertarProducto("Amoxicilina", 5, BdPrueba.mes(2));

        Venta venta = controller.registrarVenta("AMOXICILINA", 1);

        assertEquals(proximo, venta.idProducto());
        assertEquals(4, bd.existencias(proximo));
    }

    @Test
    @DisplayName("TC-05: eliminar varios registros continúa aunque alguno falle y reporta cuáles")
    void tc05_loteConErrores() throws Exception {
        int a = bd.insertarProducto("A", 1, BdPrueba.vigente());
        int b = bd.insertarProducto("B", 1, BdPrueba.vigente());

        List<String> errores = controller.eliminarProductos(List.of(a, 9999, b));

        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("9999"), errores.get(0));
        assertTrue(controller.obtenerProductos().isEmpty(), "A y B se eliminaron");
    }

    @Test
    @DisplayName("TC-06: apartarProducto por ID o nombre; editar y eliminar el apartado")
    void tc06_apartados() throws Exception {
        int id = bd.insertarProducto("Ketoprofeno", 3, BdPrueba.vigente());

        controller.apartarProducto(String.valueOf(id), LocalDate.of(2026, 1, 10));
        controller.editarApartado(id, LocalDate.of(2026, 1, 15));
        assertEquals("2026-01-15", bd.fechaApartado(id));

        assertTrue(controller.eliminarApartados(List.of(id)).isEmpty());
        assertTrue(controller.obtenerProductosApartados().isEmpty());
    }
}
