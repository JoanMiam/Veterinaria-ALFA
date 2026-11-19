package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-009-01 — Integridad de datos (FIX-009).
 *
 * <p>Cubre: transacción en {@code editarVenta}, devolución de stock en
 * {@code eliminarVenta}, borrado lógico de productos, migración del esquema,
 * búsqueda por nombre (mayúsculas y lotes FEFO) y escape HTML en reportes.
 *
 * <p>Usa SQLite en memoria. Los flujos de error del DAO todavía muestran
 * {@code JOptionPane}; en modo headless lanzan {@code HeadlessException} y lo que
 * se verifica es el estado de la BD.
 */
@DisplayName("PRU-009-01 | InventarioDAO — Integridad de datos (FIX-009)")
class InventarioDAOIntegridadTest {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    private Connection connection;
    private InventarioDAO dao;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        dao = new InventarioDAO(connection); // crea el esquema actual
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

    private static String vigente() {
        return YearMonth.now().plusMonths(6).format(YM);
    }

    private int insertarProducto(String nombre, int existencias, String caducidad) throws SQLException {
        String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, existencias);
            ps.setString(3, "L-" + nombre);
            ps.setString(4, caducidad);
            ps.setString(5, "2026-01-01");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private int existencias(int idProducto) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT existencias FROM productos WHERE id = ?")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "El producto " + idProducto + " debe existir");
                return rs.getInt(1);
            }
        }
    }

    private int idUltimaVenta() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(id) FROM historial_ventas")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private int cantidadVenta(int idVenta) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT cantidad FROM historial_ventas WHERE id = ?")) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private int contar(String sql) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void ignorarDialogo(Runnable accion) {
        try {
            accion.run();
        } catch (java.awt.HeadlessException esperado) {
            // El DAO todavía muestra JOptionPane en errores; se verifica el estado de la BD.
        }
    }

    // =========================================================================
    // editarVenta: transacción
    // =========================================================================

    @Test
    @DisplayName("TC-01: editarVenta al aumentar la cantidad descuenta la diferencia del stock")
    void tc01_editarVenta_aumentaCantidad_ajustaStock() throws SQLException {
        int id = insertarProducto("Amoxicilina", 50, vigente());
        assertTrue(dao.registrarVenta(id, "Amoxicilina", 5));
        int venta = idUltimaVenta();

        assertTrue(dao.editarVenta(venta, "Amoxicilina", "8", LocalDate.now().toString()));

        assertEquals(42, existencias(id), "50 - 8 = 42");
        assertEquals(8, cantidadVenta(venta));
    }

    @Test
    @DisplayName("TC-02: si falla la actualización de la venta, el ajuste de stock se deshace")
    void tc02_editarVenta_falloEnVenta_haceRollbackDelStock() throws SQLException {
        int id = insertarProducto("Amoxicilina", 50, vigente());
        assertTrue(dao.registrarVenta(id, "Amoxicilina", 5));
        int venta = idUltimaVenta();
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TRIGGER falla_venta BEFORE UPDATE ON historial_ventas "
                    + "BEGIN SELECT RAISE(ABORT, 'falla simulada'); END");
        }

        ignorarDialogo(() -> assertFalse(dao.editarVenta(venta, "Amoxicilina", "20", LocalDate.now().toString())));

        assertEquals(45, existencias(id), "El stock debe quedar como antes de la edición (50 - 5)");
        assertEquals(5, cantidadVenta(venta), "La venta no debe cambiar");
    }

    @Test
    @DisplayName("TC-03: editarVenta rechaza cantidad 0 sin tocar stock ni venta")
    void tc03_editarVenta_cantidadCero_seRechaza() throws SQLException {
        int id = insertarProducto("Amoxicilina", 50, vigente());
        assertTrue(dao.registrarVenta(id, "Amoxicilina", 5));
        int venta = idUltimaVenta();

        ignorarDialogo(() -> assertFalse(dao.editarVenta(venta, "Amoxicilina", "0", LocalDate.now().toString())));

        assertEquals(45, existencias(id));
        assertEquals(5, cantidadVenta(venta));
    }

    @Test
    @DisplayName("TC-04: editarVenta rechaza un aumento mayor al stock disponible")
    void tc04_editarVenta_aumentoSinStock_seRechaza() throws SQLException {
        int id = insertarProducto("Amoxicilina", 10, vigente());
        assertTrue(dao.registrarVenta(id, "Amoxicilina", 5));
        int venta = idUltimaVenta();

        ignorarDialogo(() -> assertFalse(dao.editarVenta(venta, "Amoxicilina", "16", LocalDate.now().toString())));

        assertEquals(5, existencias(id));
        assertEquals(5, cantidadVenta(venta));
    }

    // =========================================================================
    // eliminarVenta: regresa stock
    // =========================================================================

    @Test
    @DisplayName("TC-05: eliminarVenta regresa la cantidad al stock y borra el registro")
    void tc05_eliminarVenta_regresaStock() throws SQLException {
        int id = insertarProducto("Paracetamol", 30, vigente());
        assertTrue(dao.registrarVenta(id, "Paracetamol", 7));
        int venta = idUltimaVenta();
        assertEquals(23, existencias(id));

        assertTrue(dao.eliminarVenta(venta));

        assertEquals(30, existencias(id), "El stock debe volver a 30");
        assertEquals(0, contar("SELECT COUNT(*) FROM historial_ventas"));
    }

    @Test
    @DisplayName("TC-06: eliminarVenta con id inexistente retorna false sin cambios")
    void tc06_eliminarVenta_idInexistente() throws SQLException {
        int id = insertarProducto("Paracetamol", 30, vigente());

        assertFalse(dao.eliminarVenta(9999));

        assertEquals(30, existencias(id));
    }

    @Test
    @DisplayName("TC-07: eliminarVenta de una venta huérfana (producto borrado en versiones previas) solo borra la venta")
    void tc07_eliminarVenta_huerfana() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (777, 'Viejo', 3, '2025-01-01')");
        }

        assertTrue(dao.eliminarVenta(idUltimaVenta()));

        assertEquals(0, contar("SELECT COUNT(*) FROM historial_ventas"));
    }

    // =========================================================================
    // eliminarProducto: borrado lógico
    // =========================================================================

    @Test
    @DisplayName("TC-08: eliminarProducto oculta el producto pero conserva su historial editable")
    void tc08_eliminarProducto_ocultaYConservaHistorial() throws SQLException {
        int id = insertarProducto("Ketoprofeno", 20, vigente());
        assertTrue(dao.registrarVenta(id, "Ketoprofeno", 4));
        int venta = idUltimaVenta();

        assertTrue(dao.eliminarProducto(id));

        assertEquals(0, dao.obtenerProductos().length, "No aparece en inventario");
        assertNull(dao.buscarProductoPorIdONombre("Ketoprofeno"), "No se encuentra por nombre");
        assertNull(dao.buscarProductoPorIdONombre(String.valueOf(id)), "No se encuentra por ID");
        assertEquals(1, dao.obtenerHistorialVentas().size(), "La venta sigue en el historial");
        assertTrue(dao.editarVenta(venta, "Ketoprofeno", "2", LocalDate.now().toString()),
                "La venta de un producto oculto se puede seguir editando");
        assertFalse(dao.eliminarProducto(id), "Eliminar dos veces retorna false");
    }

    @Test
    @DisplayName("TC-09: no se puede vender un producto eliminado")
    void tc09_registrarVenta_productoEliminado() throws SQLException {
        int id = insertarProducto("Ketoprofeno", 20, vigente());
        assertTrue(dao.eliminarProducto(id));

        ignorarDialogo(() -> assertFalse(dao.registrarVenta(id, "Ketoprofeno", 1)));

        assertEquals(20, existencias(id));
        assertEquals(0, contar("SELECT COUNT(*) FROM historial_ventas"));
    }

    // =========================================================================
    // Migración de esquema
    // =========================================================================

    @Test
    @DisplayName("TC-10: una BD con el esquema anterior recibe la columna 'activo' sin perder datos")
    void tc10_migracion_agregaColumnaActivo() throws SQLException {
        try (Connection vieja = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement st = vieja.createStatement()) {
            st.execute("CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, existencias INTEGER, "
                    + "lote TEXT, caducidad TEXT, fechaEntrada TEXT, fecha_separado TEXT)");
            st.execute("INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) "
                    + "VALUES ('Meloxicam', 12, 'L9', '" + vigente() + "', '2025-01-01')");

            InventarioDAO daoViejo = new InventarioDAO(vieja);

            Object[][] productos = daoViejo.obtenerProductos();
            assertEquals(1, productos.length, "El producto existente sigue visible");
            assertEquals("Meloxicam", productos[0][1]);
            assertEquals(7, productos[0].length, "Las tablas siguen recibiendo 7 columnas");
            try (ResultSet rs = st.executeQuery("SELECT activo FROM productos")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Los productos existentes quedan activos");
            }
        }
    }

    // =========================================================================
    // Búsqueda por nombre y lote
    // =========================================================================

    @Test
    @DisplayName("TC-11: la búsqueda por nombre ignora mayúsculas y espacios, incluso con acentos")
    void tc11_busqueda_ignoraMayusculas() throws SQLException {
        int id = insertarProducto("Ácido Tranexámico", 10, vigente());

        Object[] r = dao.buscarProductoPorIdONombre("  ÁCIDO tranexámico ");

        assertNotNull(r);
        assertEquals(id, r[0]);
        assertEquals("Ácido Tranexámico", r[1], "Devuelve el nombre real registrado");
    }

    @Test
    @DisplayName("TC-12: con varios lotes elige el vendible que caduca primero (FEFO)")
    void tc12_busqueda_variosLotes_eligeFefo() throws SQLException {
        insertarProducto("Amoxicilina", 10, YearMonth.now().minusMonths(2).format(YM)); // caducado
        insertarProducto("Amoxicilina", 0, YearMonth.now().plusMonths(1).format(YM));   // sin stock
        insertarProducto("Amoxicilina", 5, YearMonth.now().plusMonths(9).format(YM));   // vigente, lejano
        int esperado = insertarProducto("Amoxicilina", 5, YearMonth.now().plusMonths(3).format(YM)); // vigente, próximo

        Object[] r = dao.buscarProductoPorIdONombre("amoxicilina");

        assertNotNull(r);
        assertEquals(esperado, r[0], "Debe elegir el lote vigente con stock que caduca primero");
        assertEquals(esperado, dao.obtenerProductoIdPorNombre("Amoxicilina"),
                "obtenerProductoIdPorNombre usa la misma regla");
        assertTrue(dao.registrarVenta((int) r[0], (String) r[1], 2), "La venta con ese lote procede");
    }

    @Test
    @DisplayName("TC-13: si ningún lote es vendible devuelve el primero (para que la venta muestre el motivo)")
    void tc13_busqueda_ningunVendible_devuelvePrimero() throws SQLException {
        int primero = insertarProducto("Amoxicilina", 10, YearMonth.now().minusMonths(2).format(YM));
        insertarProducto("Amoxicilina", 10, YearMonth.now().minusMonths(1).format(YM));

        Object[] r = dao.buscarProductoPorIdONombre("Amoxicilina");

        assertNotNull(r);
        assertEquals(primero, r[0]);
    }

    // =========================================================================
    // Reportes HTML
    // =========================================================================

    @Test
    @DisplayName("TC-14: los reportes HTML escapan el texto de los productos y la clínica")
    void tc14_exportarHtml_escapaTexto() throws SQLException, IOException {
        insertarProducto("Amoxi <500mg> & \"Plus\"", 3, vigente());
        dao.guardarConfiguracion(null, "Vet <ALFA>", null, null, null, null);
        File tmp = File.createTempFile("inventario_escape_", ".html");
        tmp.deleteOnExit();

        assertTrue(dao.exportarInventarioHTML(tmp));

        String html = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        assertTrue(html.contains("Amoxi &lt;500mg&gt; &amp; &quot;Plus&quot;"), "Nombre de producto escapado");
        assertTrue(html.contains("Vet &lt;ALFA&gt;"), "Nombre de la clínica escapado");
        assertFalse(html.contains("<500mg>"), "No debe quedar HTML sin escapar");
    }

    @Test
    @DisplayName("TC-15: el reporte de inventario no incluye productos eliminados")
    void tc15_exportarHtml_omiteEliminados() throws SQLException, IOException {
        insertarProducto("Visible", 3, vigente());
        int oculto = insertarProducto("Oculto", 3, vigente());
        assertTrue(dao.eliminarProducto(oculto));
        File tmp = File.createTempFile("inventario_activos_", ".html");
        tmp.deleteOnExit();

        assertTrue(dao.exportarInventarioHTML(tmp));

        String html = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        assertTrue(html.contains("Visible"));
        assertFalse(html.contains("Oculto"));
    }
}
