package prueba;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import model.InventarioDAO;

/**
 * [MR-010] Base de datos SQLite en memoria para las pruebas, con utilidades para
 * insertar datos y consultar el estado directamente (sin pasar por el DAO).
 *
 * <p>Las caducidades se expresan relativas al mes actual ({@link #mes(int)}) para que
 * las pruebas no dejen de pasar con el paso del tiempo.
 */
public final class BdPrueba implements AutoCloseable {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");

    public final Connection conexion;
    public final InventarioDAO dao;

    private BdPrueba(Connection conexion) {
        this.conexion = conexion;
        this.dao = new InventarioDAO(conexion);
    }

    /** BD vacía con el esquema actual. */
    public static BdPrueba nueva() throws SQLException {
        return new BdPrueba(DriverManager.getConnection("jdbc:sqlite::memory:"));
    }

    /**
     * BD creada con el esquema de versiones anteriores (sin {@code activo}, sin
     * {@code user_version}) y el producto indicado, antes de abrirla con el DAO.
     */
    public static BdPrueba conEsquemaAnterior(String nombre, int existencias, String caducidad) throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT, existencias INTEGER, "
                    + "lote TEXT, caducidad TEXT, fechaEntrada TEXT, fecha_separado TEXT)");
            st.execute("CREATE TABLE historial_ventas (id INTEGER PRIMARY KEY AUTOINCREMENT, idProducto INTEGER, "
                    + "nombre TEXT, cantidad INTEGER, fechaVenta TEXT, "
                    + "FOREIGN KEY (idProducto) REFERENCES productos(id) ON DELETE CASCADE)");
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, 'L-OLD', ?, '2025-01-01')")) {
            ps.setString(1, nombre);
            ps.setInt(2, existencias);
            ps.setString(3, caducidad);
            ps.executeUpdate();
        }
        return new BdPrueba(c);
    }

    /** @return mes de caducidad {@code yyyy-MM} desplazado {@code meses} desde el actual. */
    public static String mes(int meses) {
        return YearMonth.now().plusMonths(meses).format(YM);
    }

    /** Caducidad claramente vigente (dentro de 6 meses). */
    public static String vigente() {
        return mes(6);
    }

    public int insertarProducto(String nombre, int existencias, String caducidad) throws SQLException {
        return insertarProducto(nombre, existencias, "L-" + nombre, caducidad);
    }

    public int insertarProducto(String nombre, int existencias, String lote, String caducidad) throws SQLException {
        String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setInt(2, existencias);
            ps.setString(3, lote);
            ps.setString(4, caducidad);
            ps.setString(5, "2026-01-01");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public int insertarVenta(int idProducto, String nombre, int cantidad, String fecha) throws SQLException {
        String sql = "INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idProducto);
            ps.setString(2, nombre);
            ps.setInt(3, cantidad);
            ps.setString(4, fecha);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public void marcarApartado(int idProducto, String fecha) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE productos SET fecha_separado = ? WHERE id = ?")) {
            ps.setString(1, fecha);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        }
    }

    public int existencias(int idProducto) throws SQLException {
        return entero("SELECT existencias FROM productos WHERE id = " + idProducto);
    }

    public int cantidadVenta(int idVenta) throws SQLException {
        return entero("SELECT cantidad FROM historial_ventas WHERE id = " + idVenta);
    }

    public String fechaApartado(int idProducto) throws SQLException {
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery("SELECT fecha_separado FROM productos WHERE id = " + idProducto)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    public int contar(String tabla) throws SQLException {
        return entero("SELECT COUNT(*) FROM " + tabla);
    }

    public void ejecutar(String sql) throws SQLException {
        try (Statement st = conexion.createStatement()) {
            st.execute(sql);
        }
    }

    /** @return primer entero del resultado, o -1 si no hay filas. */
    public int entero(String sql) throws SQLException {
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    @Override
    public void close() throws SQLException {
        conexion.close();
    }
}
