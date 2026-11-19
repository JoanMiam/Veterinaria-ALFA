package model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * PRU-004-01 — Validación de caducidad y existencias en separarProducto (MR-004, MR-008).
 *
 * <p>[MR-010] Los rechazos se verifican con {@link OperacionRechazadaException}.
 */
@DisplayName("PRU-004-01 | InventarioDAO — validación en separarProducto (MR-004)")
class InventarioDAOSepararProductoTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 5, 18);

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

    @Test
    @DisplayName("TC-01: Producto vigente con existencias → apartado registrado")
    void tc01_productoVigente_conExistencias_registra() throws SQLException {
        int id = bd.insertarProducto("Amoxicilina", 10, BdPrueba.mes(3));

        assertDoesNotThrow(() -> dao.separarProducto(id, FECHA));

        assertEquals("2026-05-18", bd.fechaApartado(id));
    }

    @Test
    @DisplayName("TC-02: Producto caducado → apartado rechazado")
    void tc02_productoCaducado_rechazado() throws SQLException {
        int id = bd.insertarProducto("Ibuprofeno", 5, BdPrueba.mes(-2));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> dao.separarProducto(id, FECHA));

        assertTrue(e.getMessage().contains("caducado"), e.getMessage());
        assertNull(bd.fechaApartado(id));
    }

    @Test
    @DisplayName("TC-03: Caducidad = mes actual → producto aún vigente, apartado permitido")
    void tc03_caducidadMesActual_permitido() throws SQLException {
        int id = bd.insertarProducto("Paracetamol", 20, BdPrueba.mes(0));

        assertDoesNotThrow(() -> dao.separarProducto(id, FECHA));
    }

    @Test
    @DisplayName("TC-04: Producto sin existencias → apartado rechazado")
    void tc04_productoSinExistencias_rechazado() throws SQLException {
        int id = bd.insertarProducto("Vitamina C", 0, BdPrueba.mes(3));

        OperacionRechazadaException e = assertThrows(OperacionRechazadaException.class,
                () -> dao.separarProducto(id, FECHA));

        assertTrue(e.getMessage().contains("existencias"), e.getMessage());
        assertNull(bd.fechaApartado(id));
    }

    @Test
    @DisplayName("TC-05: ID de producto inexistente → apartado rechazado")
    void tc05_productoInexistente_rechazado() {
        assertThrows(OperacionRechazadaException.class, () -> dao.separarProducto(9999, FECHA));
    }

    @Test
    @DisplayName("TC-06: Producto con caducidad corrupta → apartado rechazado")
    void tc06_caducidadCorrupta_rechazado() throws SQLException {
        int id = bd.insertarProducto("Med-Basura", 10, "no-es-fecha");

        assertThrows(OperacionRechazadaException.class, () -> dao.separarProducto(id, FECHA));

        assertNull(bd.fechaApartado(id));
    }
}
