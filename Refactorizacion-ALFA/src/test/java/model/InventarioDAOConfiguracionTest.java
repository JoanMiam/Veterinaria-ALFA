package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-005-01 — Pruebas Unitarias: guardarConfiguracion, obtenerConfiguracion
 * y exportación HTML (inventario, ventas, apartados) en InventarioDAO (MR-005).
 *
 * <p>Usa SQLite en memoria para aislamiento total. El esquema incluye las tablas
 * productos, historial_ventas y configuracion_veterinaria creadas manualmente
 * para no depender de crearBaseDeDatos().
 */
@DisplayName("PRU-005-01 | InventarioDAO — configuracion y exportación HTML (MR-005)")
class InventarioDAOConfiguracionTest {

    private Connection connection;
    private InventarioDAO dao;

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
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS historial_ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, idProducto INTEGER, " +
                "nombre TEXT, cantidad INTEGER, fechaVenta TEXT)"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS configuracion_veterinaria (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, logo TEXT, nombre TEXT, " +
                "direccion TEXT, telefono TEXT, rfc TEXT, horarios TEXT)"
            );
        }
    }

    private int insertarProducto(String nombre, int existencias) throws SQLException {
        String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, existencias);
            ps.setString(3, "L-TEST");
            ps.setString(4, "2027-12");
            ps.setString(5, "2026-01-01");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    private void insertarVenta(int idProducto, String nombre, int cantidad) throws SQLException {
        String sql = "INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setString(2, nombre);
            ps.setInt(3, cantidad);
            ps.setString(4, "2026-05-22");
            ps.executeUpdate();
        }
    }

    private void marcarApartado(int idProducto, String fecha) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE productos SET fecha_separado = ? WHERE id = ?")) {
            ps.setString(1, fecha);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        }
    }

    private long contarRegistrosConfig() throws SQLException {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM configuracion_veterinaria")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private String leerNombreConfig() throws SQLException {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT nombre FROM configuracion_veterinaria WHERE id=1")) {
            return rs.next() ? rs.getString("nombre") : null;
        }
    }

    private String contenidoArchivo(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    // =========================================================================
    // TC-01: guardarConfiguracion — primera inserción crea registro con id=1
    // =========================================================================

    @Test
    @DisplayName("TC-01: guardarConfiguracion primera inserción crea exactamente un registro con id=1")
    void tc01_guardarConfiguracion_primeraInsercion() throws SQLException {
        dao.guardarConfiguracion(null, "Vet ALFA", "Calle 1", "9991234567", "ALFA010101AAA", "L-V 9-18");

        assertEquals(1, contarRegistrosConfig(), "Debe existir exactamente un registro en configuracion_veterinaria");
        assertEquals("Vet ALFA", leerNombreConfig(), "El nombre guardado debe coincidir");
    }

    // =========================================================================
    // TC-02: guardarConfiguracion — INSERT OR REPLACE no genera duplicados
    // =========================================================================

    @Test
    @DisplayName("TC-02: guardarConfiguracion segunda llamada actualiza sin duplicar registros")
    void tc02_guardarConfiguracion_actualizaSinDuplicar() throws SQLException {
        dao.guardarConfiguracion(null, "Vet ALFA", "Calle 1", "999", "RFC1", "L-V");
        dao.guardarConfiguracion(null, "Vet BETA", "Calle 2", "888", "RFC2", "L-S");

        assertEquals(1, contarRegistrosConfig(), "No debe haber duplicados tras segunda llamada");
        assertEquals("Vet BETA", leerNombreConfig(), "Nombre debe reflejar la última actualización");
    }

    // =========================================================================
    // TC-03: obtenerConfiguracion — retorna valores correctos cuando existe config
    // =========================================================================

    @Test
    @DisplayName("TC-03: obtenerConfiguracion retorna Object[6] con valores guardados")
    void tc03_obtenerConfiguracion_conDatos() throws SQLException {
        dao.guardarConfiguracion(null, "Vet ALFA", "Av. Principal", "9998887766", "ALFA010101BBB", "L-D 8-20");

        Object[] config = dao.obtenerConfiguracion();

        assertNotNull(config, "No debe retornar null cuando existe configuración");
        assertEquals(6, config.length, "Debe retornar arreglo de 6 elementos");
        assertNull(config[0], "logo debe ser null si no se guardó");
        assertEquals("Vet ALFA",          config[1], "nombre debe coincidir");
        assertEquals("Av. Principal",     config[2], "direccion debe coincidir");
        assertEquals("9998887766",        config[3], "telefono debe coincidir");
        assertEquals("ALFA010101BBB",     config[4], "rfc debe coincidir");
        assertEquals("L-D 8-20",          config[5], "horarios deben coincidir");
    }

    // =========================================================================
    // TC-04: obtenerConfiguracion — retorna null cuando no hay configuración
    // =========================================================================

    @Test
    @DisplayName("TC-04: obtenerConfiguracion retorna null cuando tabla vacía")
    void tc04_obtenerConfiguracion_sinDatos() {
        Object[] config = dao.obtenerConfiguracion();

        assertNull(config, "Debe retornar null si no existe ningún registro de configuración");
    }

    // =========================================================================
    // TC-05: exportarInventarioHTML — genera archivo con datos de productos
    // =========================================================================

    @Test
    @DisplayName("TC-05: exportarInventarioHTML genera archivo HTML con datos de productos")
    void tc05_exportarInventarioHTML_conProductos() throws SQLException, IOException {
        insertarProducto("Amoxicilina", 10);
        insertarProducto("Paracetamol", 5);
        dao.guardarConfiguracion(null, "Vet ALFA", null, null, null, null);

        File tmp = File.createTempFile("inventario_test_", ".html");
        tmp.deleteOnExit();

        dao.exportarInventarioHTML(tmp);

        assertTrue(tmp.exists() && tmp.length() > 0, "El archivo HTML debe existir y no estar vacío");
        String contenido = contenidoArchivo(tmp);
        assertTrue(contenido.contains("<table"), "Debe contener elemento <table>");
        assertTrue(contenido.contains("Amoxicilina"), "Debe contener nombre del primer producto");
        assertTrue(contenido.contains("Paracetamol"), "Debe contener nombre del segundo producto");
        assertTrue(contenido.contains("Vet ALFA"), "Debe incluir nombre de la clínica en encabezado");
    }

    // =========================================================================
    // TC-06: exportarVentasHTML — genera archivo con historial de ventas
    // =========================================================================

    @Test
    @DisplayName("TC-06: exportarVentasHTML genera archivo HTML con historial de ventas")
    void tc06_exportarVentasHTML_conVentas() throws SQLException, IOException {
        int idProd = insertarProducto("Ibuprofeno", 20);
        insertarVenta(idProd, "Ibuprofeno", 3);

        File tmp = File.createTempFile("ventas_test_", ".html");
        tmp.deleteOnExit();

        dao.exportarVentasHTML(tmp);

        assertTrue(tmp.exists() && tmp.length() > 0, "El archivo HTML debe existir y no estar vacío");
        String contenido = contenidoArchivo(tmp);
        assertTrue(contenido.contains("<table"), "Debe contener elemento <table>");
        assertTrue(contenido.contains("Ibuprofeno"), "Debe contener el nombre del producto vendido");
    }

    // =========================================================================
    // TC-07: exportarVentasHTML sin configuración — encabezado genérico sin nombre de clínica
    // =========================================================================

    @Test
    @DisplayName("TC-07: exportarVentasHTML sin configuración genera encabezado genérico")
    void tc07_exportarVentasHTML_sinConfiguracion() throws SQLException, IOException {
        int idProd = insertarProducto("Vitamina C", 15);
        insertarVenta(idProd, "Vitamina C", 1);

        File tmp = File.createTempFile("ventas_sin_config_", ".html");
        tmp.deleteOnExit();

        dao.exportarVentasHTML(tmp);

        String contenido = contenidoArchivo(tmp);
        assertTrue(contenido.contains("<table"), "Debe contener tabla aunque no haya configuración");
        assertFalse(contenido.contains("data:image/png;base64,"),
            "No debe incluir logo si no existe configuración");
    }

    // =========================================================================
    // TC-08: exportarApartadosHTML — genera archivo con productos apartados
    // =========================================================================

    @Test
    @DisplayName("TC-08: exportarApartadosHTML genera archivo HTML con productos apartados")
    void tc08_exportarApartadosHTML_conApartados() throws SQLException, IOException {
        int idProd = insertarProducto("Ketamina", 8);
        marcarApartado(idProd, "2026-05-22");

        File tmp = File.createTempFile("apartados_test_", ".html");
        tmp.deleteOnExit();

        dao.exportarApartadosHTML(tmp);

        assertTrue(tmp.exists() && tmp.length() > 0, "El archivo HTML debe existir y no estar vacío");
        String contenido = contenidoArchivo(tmp);
        assertTrue(contenido.contains("<table"), "Debe contener elemento <table>");
        assertTrue(contenido.contains("Ketamina"), "Debe contener el nombre del producto apartado");
        assertTrue(contenido.contains("2026-05-22"), "Debe contener la fecha de apartado");
    }
}
