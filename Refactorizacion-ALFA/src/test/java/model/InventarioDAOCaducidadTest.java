package model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import prueba.BdPrueba;

/**
 * PRU-003-01 — Validación de caducidad en registrarVenta y regla de expiración en
 * obtenerMedicamentosProximosACaducar / obtenerMedicamentosCaducados (MR-003, MR-008).
 *
 * <p>[MR-010] Los flujos de error ya no muestran diálogos: lanzan
 * {@link OperacionRechazadaException}, así que se verifican directamente con su mensaje.
 */
@DisplayName("PRU-003-01 | InventarioDAO — validación de caducidad (MR-003)")
class InventarioDAOCaducidadTest {

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

    /** Caducidad que ya expiró (hace 2 meses). */
    private static String caducidadExpirada() {
        return BdPrueba.mes(-2);
    }

    /** Caducidad = mes actual: expira el 1.º del mes que viene, todavía vigente. */
    private static String caducidadMesActual() {
        return BdPrueba.mes(0);
    }

    /** Caducidad dentro de 3 meses. */
    private static String caducidadFutura() {
        return BdPrueba.mes(3);
    }

    // =========================================================================
    // TC-01 a TC-04: registrarVenta — validación de caducidad
    // =========================================================================

    @Test
    @DisplayName("TC-01: Producto vigente → registrarVenta procede")
    void tc01_productoVigente_registraVenta() throws SQLException {
        int id = bd.insertarProducto("Amoxicilina", 50, caducidadFutura());

        assertDoesNotThrow(() -> dao.registrarVenta(id, 5));
        assertEquals(1, bd.contar("historial_ventas"));
    }

    @Test
    @DisplayName("TC-02: Producto con caducidad = mes actual → aún vigente, venta permitida")
    void tc02_caducidadMesActual_ventaPermitida() throws SQLException {
        int id = bd.insertarProducto("Paracetamol", 30, caducidadMesActual());

        assertDoesNotThrow(() -> dao.registrarVenta(id, 1));
    }

    @Test
    @DisplayName("TC-03: Producto caducado (mes ya pasó) → venta rechazada sin registrar")
    void tc03_productoCaducado_ventaRechazada() throws SQLException {
        int id = bd.insertarProducto("Ibuprofeno", 20, caducidadExpirada());

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> dao.registrarVenta(id, 1));

        assertTrue(e.getMessage().contains("caducado"), e.getMessage());
        assertEquals(0, bd.contar("historial_ventas"));
        assertEquals(20, bd.existencias(id));
    }

    @Test
    @DisplayName("TC-04: Producto vigente → existencias se decrementan tras la venta")
    void tc04_productoVigente_existenciasDecrementan() throws Exception {
        int id = bd.insertarProducto("Vitamina C", 100, caducidadFutura());

        dao.registrarVenta(id, 10);

        assertEquals(90, bd.existencias(id));
    }

    // =========================================================================
    // TC-05 a TC-08: próximos a caducar / caducados
    // =========================================================================

    @Test
    @DisplayName("TC-05: Producto con caducidad = mes actual → aparece en próximos a caducar")
    void tc05_caducidadMesActual_apareceEnProximos() throws SQLException {
        bd.insertarProducto("Med-A", 10, caducidadMesActual());

        assertFalse(dao.obtenerMedicamentosProximosACaducar(31).isEmpty());
    }

    @Test
    @DisplayName("TC-06: Producto caducado → NO aparece en próximos a caducar")
    void tc06_caducidadExpirada_noApareceEnProximos() throws SQLException {
        bd.insertarProducto("Med-B", 10, caducidadExpirada());

        assertTrue(dao.obtenerMedicamentosProximosACaducar(31).isEmpty());
    }

    @Test
    @DisplayName("TC-07: Producto con caducidad = mes actual → NO aparece en caducados")
    void tc07_caducidadMesActual_noApareceEnCaducados() throws SQLException {
        bd.insertarProducto("Med-C", 10, caducidadMesActual());

        assertTrue(dao.obtenerMedicamentosCaducados().isEmpty());
    }

    @Test
    @DisplayName("TC-08: Producto caducado → aparece en caducados")
    void tc08_caducidadExpirada_apareceEnCaducados() throws SQLException {
        bd.insertarProducto("Med-D", 10, caducidadExpirada());

        assertFalse(dao.obtenerMedicamentosCaducados().isEmpty());
    }

    // =========================================================================
    // TC-09 a TC-13: caducidad corrupta (MR-008)
    // =========================================================================

    @Test
    @DisplayName("TC-09: registrarVenta con caducidad null → rechazada sin registrar")
    void tc09_caducidadNull_ventaRechazada() throws SQLException {
        int id = bd.insertarProducto("Med-Null", 10, null);

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> dao.registrarVenta(id, 1));

        assertTrue(e.getMessage().contains("formato válido"), e.getMessage());
        assertEquals(0, bd.contar("historial_ventas"));
    }

    @Test
    @DisplayName("TC-10: registrarVenta con caducidad vacía → rechazada sin registrar")
    void tc10_caducidadVacia_ventaRechazada() throws SQLException {
        int id = bd.insertarProducto("Med-Vacio", 10, "");

        assertThrows(OperacionRechazadaException.class, () -> dao.registrarVenta(id, 1));
        assertEquals(0, bd.contar("historial_ventas"));
    }

    @Test
    @DisplayName("TC-11: registrarVenta con caducidad mal formada → rechazada sin registrar")
    void tc11_caducidadMalFormada_ventaRechazada() throws SQLException {
        int id = bd.insertarProducto("Med-Basura", 10, "2025-13");

        assertThrows(OperacionRechazadaException.class, () -> dao.registrarVenta(id, 1));
        assertEquals(0, bd.contar("historial_ventas"));
    }

    @Test
    @DisplayName("TC-12: obtenerMedicamentosProximosACaducar omite fila corrupta y devuelve el resto")
    void tc12_proximosACaducar_omiteCorrupto() throws SQLException {
        bd.insertarProducto("Med-Corrupto", 10, "no-es-fecha");
        bd.insertarProducto("Med-Valido", 10, caducidadMesActual());

        List<Producto> proximos = dao.obtenerMedicamentosProximosACaducar(31);

        assertEquals(1, proximos.size());
        assertEquals("Med-Valido", proximos.get(0).nombre());
    }

    @Test
    @DisplayName("TC-13: obtenerMedicamentosCaducados omite fila corrupta y devuelve el resto")
    void tc13_caducados_omiteCorrupto() throws SQLException {
        bd.insertarProducto("Med-Corrupto", 10, null);
        bd.insertarProducto("Med-Caducado", 10, caducidadExpirada());

        List<Producto> caducados = dao.obtenerMedicamentosCaducados();

        assertEquals(1, caducados.size());
        assertEquals("Med-Caducado", caducados.get(0).nombre());
    }
}
