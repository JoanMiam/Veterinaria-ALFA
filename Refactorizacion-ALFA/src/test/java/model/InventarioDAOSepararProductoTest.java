package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-004-01 — Pruebas Unitarias: validación de caducidad y existencias en
 * separarProducto y método auxiliar estaCaducado (MR-004).
 *
 * <p>Usa SQLite en memoria para aislamiento total. Los flujos que invocan
 * JOptionPane en entorno headless lanzan HeadlessException; en esos casos
 * la prueba valida el estado de la BD en lugar del valor de retorno.
 */
@DisplayName("PRU-004-01 | InventarioDAO — validación en separarProducto (MR-004)")
class InventarioDAOSepararProductoTest {

    private Connection connection;
    private InventarioDAO dao;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        crearEsquema();
        dao = new InventarioDAO(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) connection.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void crearEsquema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, " +
                "existencias INTEGER, lote TEXT, caducidad TEXT, " +
                "fechaEntrada TEXT, fecha_separado TEXT)"
            );
        }
    }

    private int insertarProducto(String nombre, int existencias, String caducidad) throws SQLException {
        String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, existencias);
            ps.setString(3, "L-TEST");
            ps.setString(4, caducidad);
            ps.setString(5, "2025-01-01");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    private String fechaSeparadoEnBD(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT fecha_separado FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("fecha_separado") : null;
        }
    }

    /** Caducidad que ya expiró (mes de hace 2 meses). */
    private String caducidadExpirada() {
        return YearMonth.now().minusMonths(2).format(FMT);
    }

    /** Caducidad = mes actual → producto aún vigente hasta que el mes pase. */
    private String caducidadMesActual() {
        return YearMonth.now().format(FMT);
    }

    /** Caducidad en 3 meses → producto claramente vigente. */
    private String caducidadFutura() {
        return YearMonth.now().plusMonths(3).format(FMT);
    }

    // =========================================================================
    // TC-01: Producto vigente + existencias > 0 → retorna true y registra apartado
    // =========================================================================

    @Test
    @DisplayName("TC-01: Producto vigente con existencias → separarProducto retorna true y registra fecha_separado")
    void tc01_productoVigente_conExistencias_retornaTrueYRegistra() throws SQLException {
        int id = insertarProducto("Amoxicilina", 10, caducidadFutura());

        boolean resultado = dao.separarProducto(id, "2026-05-18");

        assertTrue(resultado, "Producto vigente con existencias debe poder apartarse");
        assertEquals("2026-05-18", fechaSeparadoEnBD(id),
            "fecha_separado debe quedar registrada en la BD tras el apartado exitoso");
    }

    // =========================================================================
    // TC-02: Producto caducado (mes ya pasó) → sin apartado en BD
    // =========================================================================

    @Test
    @DisplayName("TC-02: Producto caducado (mes ya pasó) → separarProducto no registra apartado")
    void tc02_productoCaducado_noRegistraApartado() throws SQLException {
        int id = insertarProducto("Ibuprofeno", 5, caducidadExpirada());

        try {
            dao.separarProducto(id, "2026-05-18");
        } catch (Exception ignored) {
            // HeadlessException esperada en entorno sin GUI; lo relevante es el estado de la BD
        }

        assertNull(fechaSeparadoEnBD(id),
            "Producto caducado no debe tener fecha_separado registrada");
    }

    // =========================================================================
    // TC-03: Caducidad = mes actual → vigente, apartado permitido
    // =========================================================================

    @Test
    @DisplayName("TC-03: Caducidad = mes actual → producto aún vigente, separarProducto retorna true")
    void tc03_caducidadMesActual_productoVigente_retornaTrue() throws SQLException {
        int id = insertarProducto("Paracetamol", 20, caducidadMesActual());

        boolean resultado = dao.separarProducto(id, "2026-05-18");

        assertTrue(resultado,
            "Producto con caducidad en el mes actual no ha expirado; el apartado debe permitirse");
    }

    // =========================================================================
    // TC-04: Producto sin existencias (existencias = 0) → sin apartado en BD
    // =========================================================================

    @Test
    @DisplayName("TC-04: Producto sin existencias → separarProducto no registra apartado")
    void tc04_productoSinExistencias_noRegistraApartado() throws SQLException {
        int id = insertarProducto("Vitamina C", 0, caducidadFutura());

        try {
            dao.separarProducto(id, "2026-05-18");
        } catch (Exception ignored) {
            // HeadlessException esperada en entorno sin GUI
        }

        assertNull(fechaSeparadoEnBD(id),
            "Producto sin existencias no debe tener fecha_separado registrada");
    }

    // =========================================================================
    // TC-05: ID de producto inexistente → retorna false
    // =========================================================================

    @Test
    @DisplayName("TC-05: ID de producto inexistente → separarProducto retorna false")
    void tc05_productoInexistente_retornaFalse() throws SQLException {
        boolean resultado = dao.separarProducto(9999, "2026-05-18");

        assertFalse(resultado, "ID inexistente debe retornar false");
    }

    // =========================================================================
    // TC-06: Caducidad corrupta → sin apartado en BD (MR-008 OPC-A)
    // =========================================================================

    @Test
    @DisplayName("TC-06: Producto con caducidad corrupta → separarProducto no registra apartado")
    void tc06_caducidadCorrupta_noRegistraApartado() throws SQLException {
        int id = insertarProducto("Med-Basura", 10, "no-es-fecha");

        try {
            dao.separarProducto(id, "2026-05-18");
        } catch (Exception ignored) {
            // HeadlessException esperada en entorno sin GUI; lo relevante es el estado de la BD
        }

        assertNull(fechaSeparadoEnBD(id),
            "Producto con caducidad corrupta no debe tener fecha_separado registrada");
    }
}
