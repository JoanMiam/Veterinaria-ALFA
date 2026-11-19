package model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import util.Registro;

/**
 * [MR-010] Crea y migra el esquema de la base de datos usando {@code PRAGMA user_version}.
 *
 * <p>Cada migración se aplica una sola vez, en orden y dentro de una transacción. Para
 * cambiar el esquema se agrega un paso nuevo al final de {@link #actualizar(Connection)}
 * y se incrementa {@link #VERSION_ACTUAL}; nunca se modifica un paso ya publicado.
 *
 * <ul>
 *   <li><b>v1</b>: esquema original (productos, historial_ventas, configuracion_veterinaria).</li>
 *   <li><b>v2</b>: {@code productos.activo} para borrado lógico (FIX-009).</li>
 * </ul>
 *
 * <p>Las BDs anteriores a este mecanismo tienen {@code user_version = 0}; los pasos
 * son idempotentes ({@code IF NOT EXISTS}, verificación de columnas), así que se
 * ponen al día sin perder datos.
 */
public final class EsquemaBD {

    /** Versión de esquema que espera esta versión de la aplicación. */
    public static final int VERSION_ACTUAL = 2;

    private static final Logger LOG = Registro.de(EsquemaBD.class);

    private EsquemaBD() { }

    /**
     * Aplica las migraciones pendientes.
     *
     * @throws SQLException si alguna migración falla (se deshace por completo).
     * @throws IllegalStateException si la BD es de una versión más nueva que la app.
     */
    public static void actualizar(Connection conexion) throws SQLException {
        int version = leerVersion(conexion);
        if (version > VERSION_ACTUAL) {
            throw new IllegalStateException("La base de datos es de una versión más nueva de la aplicación (esquema v"
                    + version + "). Actualice ALFA-INVENTORY.");
        }
        if (version == VERSION_ACTUAL) {
            return;
        }

        boolean autoCommitPrevio = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try (Statement stmt = conexion.createStatement()) {
            if (version < 1) {
                v1EsquemaInicial(stmt);
            }
            if (version < 2) {
                v2BorradoLogico(conexion, stmt);
            }
            stmt.execute("PRAGMA user_version = " + VERSION_ACTUAL);
            conexion.commit();
            LOG.info("Esquema de base de datos actualizado de v" + version + " a v" + VERSION_ACTUAL);
        } catch (SQLException | RuntimeException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(autoCommitPrevio);
        }
    }

    /** @return valor de {@code PRAGMA user_version}. */
    public static int leerVersion(Connection conexion) throws SQLException {
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void v1EsquemaInicial(Statement stmt) throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS productos ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT, "
                + "existencias INTEGER, "
                + "lote TEXT, "
                + "caducidad TEXT, "
                + "fechaEntrada TEXT, "
                + "fecha_separado TEXT)");
        // FK sin ON DELETE CASCADE: nunca se borra historial en cascada.
        stmt.execute("CREATE TABLE IF NOT EXISTS historial_ventas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "idProducto INTEGER, "
                + "nombre TEXT, "
                + "cantidad INTEGER, "
                + "fechaVenta TEXT, "
                + "FOREIGN KEY (idProducto) REFERENCES productos(id))");
        stmt.execute("CREATE TABLE IF NOT EXISTS configuracion_veterinaria ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "logo TEXT, "
                + "nombre TEXT, "
                + "direccion TEXT, "
                + "telefono TEXT, "
                + "rfc TEXT, "
                + "horarios TEXT)");
    }

    private static void v2BorradoLogico(Connection conexion, Statement stmt) throws SQLException {
        if (!columnaExiste(conexion, "productos", "activo")) {
            stmt.execute("ALTER TABLE productos ADD COLUMN activo INTEGER NOT NULL DEFAULT 1");
        }
    }

    static boolean columnaExiste(Connection conexion, String tabla, String columna) throws SQLException {
        try (Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
