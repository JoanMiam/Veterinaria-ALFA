package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-002-01 — Pruebas Unitarias para InventarioDAO.buscarProductoPorIdONombre (MR-002)
 *
 * Verifica la lógica de búsqueda dual (ID numérico o nombre exacto) del DAO
 * usando una base de datos SQLite en memoria para aislamiento total.
 */
@DisplayName("PRU-002-01 | InventarioDAO — buscarProductoPorIdONombre (MR-002)")
class InventarioDAOBusquedaTest {

    private Connection connection;
    private InventarioDAO dao;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        crearEsquema();
        insertarProductosDePrueba();
        dao = new InventarioDAO(connection);
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

            ps.setString(1, "Paracetamol"); ps.setInt(2, 30);
            ps.setString(3, "L002"); ps.setString(4, "2027-06"); ps.setString(5, "2025-01-01");
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // TC-01: Búsqueda por ID numérico válido
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-01: ID numérico válido → retorna Object[]{id, nombre} correcto")
    void tc01_idNumericoValido_retornaProducto() {
        Object[] resultado = dao.buscarProductoPorIdONombre("1");

        assertNotNull(resultado, "Con id=1 existente debe retornar un arreglo, no null");
        assertEquals(1, resultado[0], "El id retornado debe ser 1");
        assertEquals("Amoxicilina", resultado[1], "El nombre retornado debe ser Amoxicilina");
    }

    // -------------------------------------------------------------------------
    // TC-02: Búsqueda por nombre exacto válido
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-02: Nombre exacto válido → retorna Object[]{id, nombre} correcto")
    void tc02_nombreExactoValido_retornaProducto() {
        Object[] resultado = dao.buscarProductoPorIdONombre("Paracetamol");

        assertNotNull(resultado, "Con nombre='Paracetamol' existente debe retornar un arreglo, no null");
        assertEquals("Paracetamol", resultado[1], "El nombre retornado debe ser Paracetamol");
    }

    // -------------------------------------------------------------------------
    // TC-03: ID numérico inexistente → null
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-03: ID numérico inexistente → retorna null")
    void tc03_idNumericoInexistente_retornaNull() {
        Object[] resultado = dao.buscarProductoPorIdONombre("9999");

        assertNull(resultado, "Un id que no existe en la BD debe retornar null");
    }

    // -------------------------------------------------------------------------
    // TC-04: Nombre inexistente → null
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-04: Nombre inexistente → retorna null")
    void tc04_nombreInexistente_retornaNull() {
        Object[] resultado = dao.buscarProductoPorIdONombre("MedicamentoInexistente");

        assertNull(resultado, "Un nombre que no existe en la BD debe retornar null");
    }

    // -------------------------------------------------------------------------
    // TC-05: Nombre parcial → null (sin LIKE, sin coincidencia parcial)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-05: Nombre parcial ('Amox') → retorna null (búsqueda exacta, sin LIKE)")
    void tc05_nombreParcial_retornaNull() {
        Object[] resultado = dao.buscarProductoPorIdONombre("Amox");

        assertNull(resultado,
            "El método usa WHERE nombre = ?, por lo que 'Amox' no debe coincidir con 'Amoxicilina'");
    }

    // -------------------------------------------------------------------------
    // TC-06: Cadena en blanco (solo espacios) → null
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-06: Cadena en blanco → retorna null")
    void tc06_cadenaEnBlanco_retornaNull() {
        Object[] resultado = dao.buscarProductoPorIdONombre("   ");

        assertNull(resultado, "Una cadena compuesta solo por espacios debe retornar null");
    }

    // -------------------------------------------------------------------------
    // TC-07: Resultado contiene exactamente dos elementos {id, nombre}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-07: Resultado válido contiene exactamente 2 elementos: {id, nombre}")
    void tc07_resultadoValido_tieneDosCampos() {
        Object[] resultado = dao.buscarProductoPorIdONombre("Amoxicilina");

        assertNotNull(resultado);
        assertEquals(2, resultado.length, "El arreglo retornado debe tener exactamente 2 elementos");
        assertInstanceOf(Integer.class, resultado[0], "El primer elemento debe ser Integer (id)");
        assertInstanceOf(String.class,  resultado[1], "El segundo elemento debe ser String (nombre)");
    }

    // -------------------------------------------------------------------------
    // TC-08: Número que es ID válido no cae en búsqueda por nombre
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-08: Entrada '2' (ID de Paracetamol) resuelve correctamente via ruta ID")
    void tc08_entradaNumerica_usaRutaId_noNombre() {
        Object[] resultado = dao.buscarProductoPorIdONombre("2");

        assertNotNull(resultado, "El id=2 existe y debe retornar resultado");
        assertEquals(2, resultado[0]);
        assertEquals("Paracetamol", resultado[1]);
    }
}
