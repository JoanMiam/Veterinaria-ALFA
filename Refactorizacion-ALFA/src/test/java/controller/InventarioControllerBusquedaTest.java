package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.InventarioDAO;

/**
 * PRU-002-02 — Pruebas Unitarias para InventarioController.buscarProductoPorIdONombre (MR-002)
 *
 * Verifica que el método wrapper del controlador delegue correctamente al DAO
 * y propague el resultado sin alterarlo, usando una BD SQLite en memoria.
 */
@DisplayName("PRU-002-02 | InventarioController — buscarProductoPorIdONombre (MR-002)")
class InventarioControllerBusquedaTest {

    private Connection connection;
    private InventarioDAO dao;
    private InventarioController controller;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        crearEsquema();
        insertarProductosDePrueba();
        dao = new InventarioDAO(connection);
        controller = new InventarioController(dao);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void crearEsquema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT, existencias INTEGER, lote TEXT, " +
                "caducidad TEXT, fechaEntrada TEXT, fecha_separado TEXT)"
            );
        }
    }

    private void insertarProductosDePrueba() throws SQLException {
        String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "Amoxicilina"); ps.setInt(2, 50);
            ps.setString(3, "L001"); ps.setString(4, "2026-12"); ps.setString(5, "2025-01-01");
            ps.executeUpdate();

            ps.setString(1, "Ibuprofeno"); ps.setInt(2, 20);
            ps.setString(3, "L003"); ps.setString(4, "2027-03"); ps.setString(5, "2025-01-01");
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // TC-01: Búsqueda por ID → el controller propaga el resultado del DAO
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-01: Búsqueda por ID numérico → el controller retorna el mismo arreglo que el DAO")
    void tc01_busquedaPorId_propagaResultadoDelDao() {
        Object[] resultado = controller.buscarProductoPorIdONombre("1");

        assertNotNull(resultado, "El controller debe propagar el resultado no-null del DAO");
        assertEquals(1,             resultado[0], "El id debe ser 1");
        assertEquals("Amoxicilina", resultado[1], "El nombre debe ser Amoxicilina");
    }

    // -------------------------------------------------------------------------
    // TC-02: Búsqueda por nombre exacto → el controller propaga el resultado del DAO
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-02: Búsqueda por nombre exacto → el controller retorna el mismo arreglo que el DAO")
    void tc02_busquedaPorNombre_propagaResultadoDelDao() {
        Object[] resultado = controller.buscarProductoPorIdONombre("Ibuprofeno");

        assertNotNull(resultado, "El controller debe propagar el resultado no-null del DAO");
        assertEquals("Ibuprofeno", resultado[1], "El nombre debe ser Ibuprofeno");
    }

    // -------------------------------------------------------------------------
    // TC-03: Sin coincidencia → el controller propaga el null del DAO
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-03: Sin coincidencia → el controller retorna null (propaga el null del DAO)")
    void tc03_sinCoincidencia_propagaNullDelDao() {
        Object[] resultado = controller.buscarProductoPorIdONombre("MedicamentoNoExiste");

        assertNull(resultado, "El controller debe propagar null cuando el DAO no encuentra resultado");
    }

    // -------------------------------------------------------------------------
    // TC-04: El resultado tiene exactamente 2 elementos sin transformación
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-04: El controller no transforma el arreglo; retorna {id, nombre} intacto")
    void tc04_resultadoNoEsTransformado() {
        Object[] resultado = controller.buscarProductoPorIdONombre("Amoxicilina");

        assertNotNull(resultado);
        assertEquals(2, resultado.length, "El wrapper no debe modificar la longitud del arreglo");
    }

    // -------------------------------------------------------------------------
    // TC-05: Nombre parcial → null propagado correctamente
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-05: Nombre parcial → null (el controller no añade lógica de coincidencia parcial)")
    void tc05_nombreParcial_propagaNullDelDao() {
        Object[] resultado = controller.buscarProductoPorIdONombre("Amox");

        assertNull(resultado,
            "El controller es un wrapper puro; no debe añadir lógica de búsqueda parcial");
    }
}
