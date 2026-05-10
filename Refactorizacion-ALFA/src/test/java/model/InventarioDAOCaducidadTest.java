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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-003-01 — Pruebas Unitarias: validación de caducidad en registrarVenta
 * y regla de expiración en obtenerMedicamentosProximosACaducar /
 * obtenerMedicamentosCaducados (MR-003 OPC-A).
 *
 * <p>Usa SQLite en memoria para aislamiento total. No hay JOptionPane en los
 * flujos de éxito; los flujos de error que llaman a JOptionPane se documentan
 * como pruebas manuales en P9_Pruebas-Funcionales_MR-003.html.
 */
@DisplayName("PRU-003-01 | InventarioDAO — validación de caducidad (MR-003)")
class InventarioDAOCaducidadTest {

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
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS historial_ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, idProducto INTEGER, " +
                "nombre TEXT, cantidad INTEGER, fechaVenta TEXT, " +
                "FOREIGN KEY (idProducto) REFERENCES productos(id) ON DELETE CASCADE)"
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

    /** Caducidad que ya expiró (mes de hace 2 meses → expiryDate = hace 1 mes). */
    private String caducidadExpirada() {
        return YearMonth.now().minusMonths(2).format(FMT);
    }

    /** Caducidad que expira este mes → expiryDate = primer día del mes que viene (futuro). */
    private String caducidadMesActual() {
        return YearMonth.now().format(FMT);
    }

    /** Caducidad que expira en 3 meses → expiryDate muy en el futuro. */
    private String caducidadFutura() {
        return YearMonth.now().plusMonths(3).format(FMT);
    }

    // =========================================================================
    // TC-01 a TC-03: registrarVenta — validación de caducidad
    // =========================================================================

    @Test
    @DisplayName("TC-01: Producto vigente → registrarVenta retorna true")
    void tc01_productoVigente_registrarVentaRetornaTrue() throws SQLException {
        int id = insertarProducto("Amoxicilina", 50, caducidadFutura());

        boolean resultado = dao.registrarVenta(id, "Amoxicilina", 5);

        assertTrue(resultado, "Producto con caducidad futura debe registrar venta sin problemas");
    }

    @Test
    @DisplayName("TC-02: Producto con caducidad = mes actual → aún vigente, venta permitida")
    void tc02_caducidadMesActual_ventaPermitida() throws SQLException {
        int id = insertarProducto("Paracetamol", 30, caducidadMesActual());

        boolean resultado = dao.registrarVenta(id, "Paracetamol", 1);

        assertTrue(resultado,
            "Un producto cuyo mes de caducidad es el actual no ha expirado aún; la venta debe permitirse");
    }

    @Test
    @DisplayName("TC-03: Producto caducado (mes ya pasó) → registrarVenta retorna false sin vender")
    void tc03_productoCaducado_registrarVentaRetornaFalse() throws SQLException {
        int id = insertarProducto("Ibuprofeno", 20, caducidadExpirada());

        // En entorno headless el JOptionPane lanza HeadlessException antes del return false.
        // Validamos que la venta no se registra comprobando historial_ventas.
        try {
            dao.registrarVenta(id, "Ibuprofeno", 1);
        } catch (Exception ignored) {
            // HeadlessException esperada; lo relevante es que no haya registro
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM historial_ventas")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "No debe existir ningún registro en historial_ventas para producto caducado");
        }
    }

    @Test
    @DisplayName("TC-04: Producto vigente → existencias se decrementan tras la venta")
    void tc04_productoVigente_existenciasDecrementan() throws SQLException {
        int id = insertarProducto("Vitamina C", 100, caducidadFutura());

        dao.registrarVenta(id, "Vitamina C", 10);

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT existencias FROM productos WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(90, rs.getInt("existencias"), "Existencias deben reducirse en la cantidad vendida");
        }
    }

    // =========================================================================
    // TC-05 a TC-08: obtenerMedicamentosProximosACaducar — nueva regla
    // =========================================================================

    @Test
    @DisplayName("TC-05: Producto con caducidad = mes actual → aparece en próximos a caducar")
    void tc05_caducidadMesActual_apareceEnProximos() throws SQLException {
        insertarProducto("Med-A", 10, caducidadMesActual());

        var proximos = dao.obtenerMedicamentosProximosACaducar(31);

        assertFalse(proximos.isEmpty(),
            "Producto con caducidad = mes actual debe aparecer en próximos a caducar");
    }

    @Test
    @DisplayName("TC-06: Producto caducado (mes ya pasó) → NO aparece en próximos a caducar")
    void tc06_caducidadExpirada_noApareceEnProximos() throws SQLException {
        insertarProducto("Med-B", 10, caducidadExpirada());

        var proximos = dao.obtenerMedicamentosProximosACaducar(31);

        assertTrue(proximos.isEmpty(),
            "Producto caducado no debe aparecer en próximos a caducar");
    }

    // =========================================================================
    // TC-07 a TC-08: obtenerMedicamentosCaducados — nueva regla
    // =========================================================================

    @Test
    @DisplayName("TC-07: Producto con caducidad = mes actual → NO aparece en caducados")
    void tc07_caducidadMesActual_noApareceEnCaducados() throws SQLException {
        insertarProducto("Med-C", 10, caducidadMesActual());

        var caducados = dao.obtenerMedicamentosCaducados();

        assertTrue(caducados.isEmpty(),
            "Producto cuyo mes de caducidad es el actual no debe aparecer como caducado");
    }

    @Test
    @DisplayName("TC-08: Producto caducado (mes ya pasó) → aparece en caducados")
    void tc08_caducidadExpirada_apareceEnCaducados() throws SQLException {
        insertarProducto("Med-D", 10, caducidadExpirada());

        var caducados = dao.obtenerMedicamentosCaducados();

        assertFalse(caducados.isEmpty(),
            "Producto cuyo mes de caducidad ya pasó debe aparecer en la lista de caducados");
    }
}
