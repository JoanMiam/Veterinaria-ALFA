package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-002-03 — Pruebas de Regresión para operaciones pre-existentes del DAO (MR-002)
 *
 * Garantiza que las operaciones de inventario y ventas que existían antes del
 * MR-002 no fueron afectadas por la introducción del método
 * {@code buscarProductoPorIdONombre}. Usa SQLite en memoria para aislamiento.
 *
 * <p><b>Alcance de regresión cubierto:</b>
 * <ul>
 *   <li>CR-01 a CR-03: {@code registrarVenta} — flujo de éxito y persistencia en BD.</li>
 *   <li>CR-04 a CR-05: {@code obtenerProductos} y {@code obtenerHistorialVentas}.</li>
 *   <li>CR-06 a CR-07: {@code agregarProducto} y {@code eliminarProducto}.</li>
 *   <li>CR-08:         {@code obtenerProductoIdPorNombre} — método previo al MR-002.</li>
 * </ul>
 *
 * <p><b>Nota:</b> Los flujos de error de {@code registrarVenta} (nombre incorrecto,
 * stock insuficiente) invocan {@code JOptionPane} y requieren entorno gráfico;
 * se documentan en P9_Pruebas_MR-002.html como pruebas manuales.
 */
@DisplayName("PRU-002-03 | Regresión — Operaciones pre-existentes en InventarioDAO (MR-002)")
class InventarioDAORegresionTest {

    private Connection connection;
    private InventarioDAO dao;

    // -------------------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        crearEsquema();
        insertarProductoDePrueba();
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
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS historial_ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "idProducto INTEGER, nombre TEXT, cantidad INTEGER, fechaVenta TEXT, " +
                "FOREIGN KEY (idProducto) REFERENCES productos(id) ON DELETE CASCADE)"
            );
        }
    }

    private void insertarProductoDePrueba() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, "Amoxicilina");
            ps.setInt(2, 50);
            ps.setString(3, "L001");
            ps.setString(4, "2026-12");
            ps.setString(5, "2025-01-01");
            ps.executeUpdate();
        }
    }

    private int contarFilas(String tabla) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int obtenerExistencias(int idProducto) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT existencias FROM productos WHERE id = ?")) {
            ps.setInt(1, idProducto);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("existencias") : -1;
        }
    }

    // =========================================================================
    // CR-01 a CR-03: registrarVenta — flujo de éxito
    // =========================================================================

    @Test
    @DisplayName("CR-01: registrarVenta(id, nombre, cantidad) retorna true en flujo de éxito")
    void cr01_registrarVenta_retornaTrue() {
        boolean resultado = dao.registrarVenta(1, "Amoxicilina", 5);

        assertTrue(resultado, "registrarVenta debe retornar true cuando id, nombre y stock son válidos");
    }

    @Test
    @DisplayName("CR-02: registrarVenta descuenta correctamente las existencias del producto")
    void cr02_registrarVenta_decrementaExistencias() throws SQLException {
        dao.registrarVenta(1, "Amoxicilina", 10);

        assertEquals(40, obtenerExistencias(1),
            "Las existencias deben reducirse en la cantidad vendida (50 - 10 = 40)");
    }

    @Test
    @DisplayName("CR-03: registrarVenta inserta un registro en historial_ventas")
    void cr03_registrarVenta_insertaEnHistorial() throws SQLException {
        dao.registrarVenta(1, "Amoxicilina", 3);

        assertEquals(1, contarFilas("historial_ventas"),
            "Debe existir un registro en historial_ventas tras la venta");
    }

    // =========================================================================
    // CR-04 a CR-05: obtenerProductos y obtenerHistorialVentas
    // =========================================================================

    @Test
    @DisplayName("CR-04: obtenerProductos retorna la lista de productos sin errores")
    void cr04_obtenerProductos_retornaDatos() {
        Object[][] productos = dao.obtenerProductos();

        assertNotNull(productos, "obtenerProductos no debe retornar null");
        assertEquals(1, productos.length, "Debe haber exactamente 1 producto en la BD de prueba");
    }

    @Test
    @DisplayName("CR-05: obtenerHistorialVentas retorna lista vacía cuando no hay ventas")
    void cr05_obtenerHistorialVentas_listaVaciaInicial() {
        var historial = dao.obtenerHistorialVentas();

        assertNotNull(historial, "obtenerHistorialVentas no debe retornar null");
        assertTrue(historial.isEmpty(), "Sin ventas registradas, la lista debe estar vacía");
    }

    // =========================================================================
    // CR-06 a CR-07: agregarProducto y eliminarProducto
    // =========================================================================

    @Test
    @DisplayName("CR-06: agregarProducto inserta correctamente un nuevo producto")
    void cr06_agregarProducto_incrementaConteo() throws SQLException {
        int antesDeAgregar = contarFilas("productos");

        dao.agregarProducto("Ibuprofeno", "20", "L002", "2027-06");

        assertEquals(antesDeAgregar + 1, contarFilas("productos"),
            "agregarProducto debe insertar una nueva fila en la tabla productos");
    }

    @Test
    @DisplayName("CR-07: eliminarProducto borra el registro y retorna true")
    void cr07_eliminarProducto_eliminaRegistroYRetornaTrue() throws SQLException {
        boolean resultado = dao.eliminarProducto(1);

        assertTrue(resultado, "eliminarProducto debe retornar true cuando el id existe");
        assertEquals(0, contarFilas("productos"),
            "La tabla productos debe quedar vacía tras eliminar el único registro");
    }

    // =========================================================================
    // CR-08: obtenerProductoIdPorNombre — método previo al MR-002
    // =========================================================================

    @Test
    @DisplayName("CR-08: obtenerProductoIdPorNombre retorna el id correcto (método pre-MR-002)")
    void cr08_obtenerProductoIdPorNombre_retornaIdCorrecto() {
        int id = dao.obtenerProductoIdPorNombre("Amoxicilina");

        assertEquals(1, id,
            "obtenerProductoIdPorNombre debe seguir devolviendo el id correcto tras MR-002");
    }

    // =========================================================================
    // CR-09: Coexistencia — buscarProductoPorIdONombre no interfiere con registrarVenta
    // =========================================================================

    @Test
    @DisplayName("CR-09: buscarProductoPorIdONombre y registrarVenta coexisten sin interferencia")
    void cr09_busquedaYRegistro_coexisten() throws SQLException {
        // Primero buscar por nombre (nueva funcionalidad)
        Object[] producto = dao.buscarProductoPorIdONombre("Amoxicilina");
        assertNotNull(producto, "La búsqueda unificada debe encontrar el producto");

        int id      = (int) producto[0];
        String nombre = (String) producto[1];

        // Luego registrar usando los datos obtenidos (flujo exacto de la vista)
        boolean registrado = dao.registrarVenta(id, nombre, 5);
        assertTrue(registrado, "La venta registrada con los datos de buscarProductoPorIdONombre debe tener éxito");

        assertEquals(45, obtenerExistencias(1),
            "Las existencias deben reducirse en 5 (50 - 5 = 45)");
    }
}
