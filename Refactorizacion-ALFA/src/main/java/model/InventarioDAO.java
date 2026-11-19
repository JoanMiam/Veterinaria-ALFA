package model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import util.RutasApp;

public class InventarioDAO {

    /** Columnas de producto que se muestran en las tablas (mismo orden que el esquema original). */
    private static final String COLUMNAS_PRODUCTO =
            "id, nombre, existencias, lote, caducidad, fechaEntrada, fecha_separado";

    private Connection connection;
    private String dbUrl;

    /**
     * Abre (o crea) la base de datos en {@code dbUrl} y deja el esquema al día.
     *
     * @throws IllegalStateException si no se puede abrir la BD o crear el esquema.
     */
    public InventarioDAO(String dbUrl) {
        this.dbUrl = dbUrl;
        this.connection = abrirConexion(dbUrl);
        inicializarEsquema();
    }

    /**
     * Constructor para pruebas (acepta conexión existente). También deja el esquema
     * al día: crea las tablas que falten y aplica migraciones.
     */
    public InventarioDAO(Connection connection) {
        this.connection = connection;
        inicializarEsquema();
    }

    /**
     * [FIX-009] Abre la BD en {@code <carpeta del ejecutable>/db/baseDeDatosInventario.db}
     * (ver {@link RutasApp}), sin depender del directorio desde donde se lanzó la app.
     *
     * @throws IllegalStateException si no se puede preparar la carpeta o abrir la BD.
     */
    public InventarioDAO() {
        this(urlPorDefecto());
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void ensureConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connection = DriverManager.getConnection(this.dbUrl);
        }
    }

    private static String urlPorDefecto() {
        try {
            return "jdbc:sqlite:" + RutasApp.prepararBaseDatos();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo preparar la carpeta de la base de datos (" + RutasApp.archivoBaseDatos() + "): "
                            + e.getMessage(), e);
        }
    }

    /**
     * [FIX-009] Antes los errores de conexión se imprimían y la conexión quedaba en
     * {@code null}, provocando un {@code NullPointerException} más adelante.
     */
    private static Connection abrirConexion(String url) {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("No se pudo abrir la base de datos (" + url + "): " + e.getMessage(), e);
        }
    }

    /** Se conserva por compatibilidad: el esquema ya se inicializa en el constructor. */
    public void crearBaseDeDatos() {
        inicializarEsquema();
    }

    /**
     * Crea las tablas que no existan y aplica las migraciones pendientes. Es idempotente.
     *
     * <p>Migraciones:
     * <ul>
     *   <li>[FIX-009] {@code productos.activo}: borrado lógico. Un producto eliminado se
     *       oculta ({@code activo = 0}) en lugar de borrarse, para conservar su historial de ventas.</li>
     * </ul>
     *
     * <p>Nota: {@code historial_ventas} declara una FK sin {@code ON DELETE CASCADE} y las
     * FKs de SQLite siguen desactivadas, así que nunca se borra historial en cascada.
     */
    private void inicializarEsquema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS productos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT, " +
                    "existencias INTEGER, " +
                    "lote TEXT, " +
                    "caducidad TEXT, " +
                    "fechaEntrada TEXT, " +
                    "fecha_separado TEXT, " +
                    "activo INTEGER NOT NULL DEFAULT 1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS historial_ventas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "idProducto INTEGER, " +
                    "nombre TEXT, " +
                    "cantidad INTEGER, " +
                    "fechaVenta TEXT, " +
                    "FOREIGN KEY (idProducto) REFERENCES productos(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS configuracion_veterinaria (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "logo TEXT, " +
                    "nombre TEXT, " +
                    "direccion TEXT, " +
                    "telefono TEXT, " +
                    "rfc TEXT, " +
                    "horarios TEXT)");
            if (!columnaExiste("productos", "activo")) {
                stmt.execute("ALTER TABLE productos ADD COLUMN activo INTEGER NOT NULL DEFAULT 1");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar el esquema de la base de datos: " + e.getMessage(), e);
        }
    }

    private boolean columnaExiste(String tabla, String columna) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Operación de BD que puede lanzar {@link SQLException}. */
    @FunctionalInterface
    private interface OperacionSql<T> {
        T ejecutar() throws SQLException;
    }

    /**
     * [FIX-009] Ejecuta {@code operacion} en una transacción: confirma si termina bien y
     * deshace todos los cambios si lanza cualquier excepción.
     */
    private <T> T enTransaccion(OperacionSql<T> operacion) throws SQLException {
        boolean autoCommitPrevio = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T resultado = operacion.ejecutar();
            connection.commit();
            return resultado;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommitPrevio);
        }
    }

    /** [FIX-009] Escapa texto para insertarlo en los reportes HTML. */
    static String escaparHtml(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void escribirArchivo(File destino, String contenido) throws IOException {
        try (FileWriter writer = new FileWriter(destino)) {
            writer.write(contenido);
        }
    }

    /**
     * [MR-005 – OPC-A] Persiste la configuración de la veterinaria en la tabla
     * {@code configuracion_veterinaria} usando {@code INSERT OR REPLACE} con {@code id = 1},
     * de modo que siempre exista como máximo un registro de configuración.
     *
     * @param logo      Imagen del logotipo codificada en Base64 (puede ser {@code null}).
     * @param nombre    Nombre de la clínica veterinaria.
     * @param direccion Dirección física de la clínica.
     * @param telefono  Teléfono de contacto.
     * @param rfc       RFC de la clínica.
     * @param horarios  Descripción de los horarios de atención.
     */
    public void guardarConfiguracion(String logo, String nombre, String direccion,
                                     String telefono, String rfc, String horarios) {
        String sql = "INSERT OR REPLACE INTO configuracion_veterinaria (id, logo, nombre, direccion, telefono, rfc, horarios) VALUES (1, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, logo);
            stmt.setString(2, nombre);
            stmt.setString(3, direccion);
            stmt.setString(4, telefono);
            stmt.setString(5, rfc);
            stmt.setString(6, horarios);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al guardar configuración: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * [MR-005 – OPC-A] Recupera la configuración de la veterinaria almacenada en
     * {@code configuracion_veterinaria}.
     *
     * @return Arreglo {@code Object[]{logo, nombre, direccion, telefono, rfc, horarios}}
     *         donde {@code logo} puede ser {@code null} si no se cargó imagen.
     *         Retorna {@code null} si no existe ningún registro de configuración.
     */
    public Object[] obtenerConfiguracion() {
        String sql = "SELECT logo, nombre, direccion, telefono, rfc, horarios FROM configuracion_veterinaria LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new Object[]{
                        rs.getString("logo"),
                        rs.getString("nombre"),
                        rs.getString("direccion"),
                        rs.getString("telefono"),
                        rs.getString("rfc"),
                        rs.getString("horarios")
                };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * [MR-005 – OPC-A] Construye el encabezado HTML con CSS embebido que comparten
     * los tres reportes (inventario, apartados, ventas). Si existe configuración de
     * veterinaria, incluye el logotipo (data-URI Base64), nombre y datos de contacto
     * antes del título del reporte.
     *
     * @param titulo Título del reporte que aparece en {@code <title>} y en {@code <h1>}.
     * @return {@link StringBuilder} con el HTML desde {@code <!DOCTYPE html>} hasta
     *         el {@code <h1>} del título, listo para agregar las filas de la tabla.
     */
    private StringBuilder buildHtmlHeader(String titulo) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">")
            .append("<title>").append(titulo).append("</title>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;font-size:13px;margin:40px 60px;color:#000;}")
            .append("h1{text-align:center;font-size:18px;font-weight:bold;margin-bottom:6px;}")
            .append(".vet-info{text-align:center;font-size:12px;margin-bottom:20px;}")
            .append("table{width:100%;border-collapse:collapse;}")
            .append("th,td{border:1px solid #000;padding:6px 10px;}")
            .append("th{background-color:#BDD7EE;font-weight:bold;text-align:center;}")
            .append("</style></head><body>\n");
        Object[] config = obtenerConfiguracion();
        if (config != null && config[1] != null) {
            if (config[0] != null && !config[0].toString().isEmpty()) {
                String b64 = config[0].toString();
                // JPG base64 starts with /9j/; PNG starts with iVBOR
                String mime = b64.startsWith("/9j/") ? "image/jpeg" : "image/png";
                html.append("<div style=\"text-align:center;margin-bottom:10px;\">")
                    .append("<img src=\"data:").append(mime).append(";base64,").append(escaparHtml(b64))
                    .append("\" style=\"max-height:80px;\"/></div>\n");
            }
            html.append("<h1>").append(escaparHtml(config[1])).append("</h1>\n");
            html.append("<div class=\"vet-info\">");
            if (config[2] != null) html.append(escaparHtml(config[2])).append(" &nbsp;|&nbsp; ");
            if (config[3] != null) html.append("Tel: ").append(escaparHtml(config[3])).append(" &nbsp;|&nbsp; ");
            if (config[4] != null) html.append("RFC: ").append(escaparHtml(config[4]));
            if (config[5] != null) html.append("<br/>Horarios: ").append(escaparHtml(config[5]));
            html.append("</div>\n");
        }
        html.append("<h1>").append(escaparHtml(titulo)).append("</h1>\n");
        return html;
    }



    /** @return productos activos (los eliminados quedan ocultos, ver {@link #eliminarProducto(int)}). */
    public Object[][] obtenerProductos() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + COLUMNAS_PRODUCTO + " FROM productos WHERE activo = 1")) {
            return rsToArray(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return new Object[0][0];
        }
    }

    public boolean agregarProducto(String nombre, String existencias, String lote, String caducidad) {
        // Validar que sea "yyyy-MM"
        if (!validarFechaCaducidad(caducidad)) {
            JOptionPane.showMessageDialog(null,
                    "La fecha de caducidad debe tener el formato yyyy-MM (ej. 2025-07).",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            ensureConnection();
            int existenciasInt = Integer.parseInt(existencias);
            if (existenciasInt < 0) {
                JOptionPane.showMessageDialog(null, "Las existencias no pueden ser negativas.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String fechaEntrada = LocalDate.now().toString();
            String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
                pstmt.setInt(2, existenciasInt);
                pstmt.setString(3, lote);
                pstmt.setString(4, caducidad); // aquí guardas "yyyy-MM"
                pstmt.setString(5, fechaEntrada);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Las existencias deben ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }





    /**
     * [FIX-009] Borrado lógico: el producto deja de aparecer en inventario, búsquedas,
     * alertas, apartados y reportes, pero la fila se conserva para que su historial de
     * ventas siga siendo consultable y editable. También se libera su apartado.
     *
     * @return {@code true} si el producto existía y estaba activo.
     */
    public boolean eliminarProducto(int id) {
        try {
            ensureConnection();
            String sql = "UPDATE productos SET activo = 0, fecha_separado = NULL WHERE id = ? AND activo = 1";
            try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean editarProducto(int id,
                                  String nombre,
                                  String existencias,
                                  String lote,
                                  String caducidad,
                                  String fechaEntrada) {
        try {
            ensureConnection();
            if (!validarFechaCaducidad(caducidad)) {
                JOptionPane.showMessageDialog(null,
                        "La fecha de caducidad debe ser yyyy-MM.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            int existenciasInt = Integer.parseInt(existencias);
            if (existenciasInt < 0) {
                JOptionPane.showMessageDialog(null, "Las existencias no pueden ser negativas.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            String sql = "UPDATE productos SET nombre = ?, existencias = ?, lote = ?, caducidad = ?, fechaEntrada = ? WHERE id = ? AND activo = 1";
            try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
                pstmt.setInt(2, existenciasInt);
                pstmt.setString(3, lote);
                pstmt.setString(4, caducidad);  // "yyyy-MM"
                pstmt.setString(5, fechaEntrada);
                pstmt.setInt(6, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Las existencias deben ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al actualizar el producto: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }



    /**
     * [MR-005 – OPC-A] Genera un reporte HTML del historial de ventas y lo guarda en
     * {@code fileToSave}. Sustituye al anterior {@code exportarCSV}. El archivo resultante
     * incluye CSS embebido y, si existe configuración de veterinaria, muestra el encabezado
     * con logotipo y datos de la clínica.
     *
     * <p>Columnas del reporte: Nombre del Medicamento, Cantidad Vendida, Fecha de Venta.
     *
     * <p>[FIX-009] Ya no muestra diálogos (la vista informa al usuario) y escapa el texto.
     *
     * @param fileToSave Ruta destino del archivo {@code .html} a generar.
     * @return {@code true} si el archivo se generó.
     */
    public boolean exportarVentasHTML(File fileToSave) {
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre, cantidad, fechaVenta FROM historial_ventas")) {
            StringBuilder html = buildHtmlHeader("Reporte de Ventas");
            html.append("<table><thead><tr>")
                .append("<th>Nombre del Medicamento</th><th>Cantidad Vendida</th><th>Fecha de Venta</th>")
                .append("</tr></thead><tbody>\n");
            while (rs.next()) {
                String fecha = rs.getString("fechaVenta");
                html.append("<tr><td>").append(escaparHtml(rs.getString("nombre"))).append("</td>")
                    .append("<td>").append(rs.getInt("cantidad")).append("</td>")
                    .append("<td>").append(fecha != null ? escaparHtml(fecha) : "N/A").append("</td></tr>\n");
            }
            html.append("</tbody></table></body></html>");
            escribirArchivo(fileToSave, html.toString());
            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parsea y valida de forma segura la cadena de caducidad del producto.
     * Centraliza la lógica de validación (regex {@code \d{4}-\d{2}}) y el parseo
     * a {@link YearMonth} reutilizando lo ya implementado en {@link #validarFechaCaducidad}.
     *
     * <p>Diseñado para invocarse desde los 4 puntos vulnerables ({@code registrarVenta},
     * {@code obtenerMedicamentosProximosACaducar}, {@code obtenerMedicamentosCaducados},
     * {@code separarProducto}). Ante {@code null} debe interpretarse según el contexto:
     * las consultas de alertas omiten el registro corrupto; las operaciones de venta/apartado
     * retornan {@code false} con un mensaje claro al usuario.
     *
     * @param fechaCaducidad cadena en formato {@code yyyy-MM} (puede ser {@code null} o vacía).
     * @return el {@link YearMonth} parseado, o {@code null} si la entrada es nula,
     *         vacía o tiene formato inválido.
     */
    private YearMonth parsearCaducidad(String fechaCaducidad) {
        return ReglasCaducidad.parsear(fechaCaducidad);
    }

    private boolean validarFechaCaducidad(String fechaCaducidad) {
        return parsearCaducidad(fechaCaducidad) != null;
    }



    private Object[][] rsToArray(ResultSet rs) throws SQLException {
        List<Object[]> dataList = new ArrayList<>();
        int columnCount = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                row[colIndex] = rs.getObject(colIndex + 1);
            }
            dataList.add(row);
        }
        return dataList.toArray(new Object[0][0]);
    }

    /**
     * Registra una venta descontando existencias del inventario y añadiendo
     * un registro en {@code historial_ventas}.
     *
     * <p>Validaciones (en orden):
     * <ol>
     *   <li>El producto con {@code id} debe existir.</li>
     *   <li>{@code nombreIngresado} debe coincidir (ignorando mayúsculas) con el nombre en BD.</li>
     *   <li>Las existencias deben ser suficientes para cubrir {@code cantidadVendida}.</li>
     *   <li>[MR-003 OPC-A] El producto no debe estar caducado. Un producto caduca al inicio
     *       del mes siguiente al indicado en su campo {@code caducidad} (formato yyyy-MM).</li>
     * </ol>
     *
     * @param id              ID del producto a vender.
     * @param nombreIngresado Nombre del medicamento tal como lo ingresó el usuario.
     * @param cantidadVendida Unidades a descontar del inventario.
     * @return {@code true} si la venta se registró exitosamente; {@code false} en caso contrario.
     */
    public boolean registrarVenta(int id, String nombreIngresado, int cantidadVendida) {
        try {
            ensureConnection();

            if (cantidadVendida <= 0) {
                JOptionPane.showMessageDialog(null, "La cantidad vendida debe ser mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Consulta el producto activo por su ID
            String nombreReal;
            int existenciasActuales;
            String caducidad;
            String selectQuery = "SELECT nombre, existencias, caducidad FROM productos WHERE id = ? AND activo = 1";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, id);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(null, "El producto con ID " + id + " no existe.", "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    nombreReal = rs.getString("nombre");
                    existenciasActuales = rs.getInt("existencias");
                    caducidad = rs.getString("caducidad");
                }
            }

            // Compara el nombre ingresado con el registrado
            if (nombreReal == null || !nombreReal.equalsIgnoreCase(nombreIngresado)) {
                JOptionPane.showMessageDialog(null, "El nombre ingresado no coincide con el producto registrado para el ID " + id + ".", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (cantidadVendida > existenciasActuales) {
                JOptionPane.showMessageDialog(null, "No hay suficientes existencias.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // [MR-003 OPC-A] Validar que el producto no esté caducado.
            // Un producto caduca al inicio del mes siguiente al indicado en caducidad.
            YearMonth cadYM = parsearCaducidad(caducidad);
            if (cadYM == null) {
                JOptionPane.showMessageDialog(null, "No se puede registrar la venta: la caducidad del producto no tiene un formato válido", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (ReglasCaducidad.estaCaducado(cadYM, LocalDate.now())) {
                JOptionPane.showMessageDialog(null, "No se puede registrar la venta: el medicamento está caducado", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Descuento de stock y registro de la venta en una sola transacción
            return enTransaccion(() -> {
                String updateQuery = "UPDATE productos SET existencias = existencias - ? WHERE id = ? AND existencias >= ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, cantidadVendida);
                    updateStmt.setInt(2, id);
                    updateStmt.setInt(3, cantidadVendida);
                    if (updateStmt.executeUpdate() == 0) {
                        throw new SQLException("Existencias insuficientes para el producto " + id);
                    }
                }

                String insertHistorial = "INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertHistorial)) {
                    insertStmt.setInt(1, id);
                    insertStmt.setString(2, nombreReal);
                    insertStmt.setInt(3, cantidadVendida);
                    insertStmt.setString(4, LocalDate.now().toString());
                    insertStmt.executeUpdate();
                }
                return true;
            });
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }






    /**
     * Edita una venta y ajusta el stock del producto por la diferencia de cantidad.
     *
     * <p>[FIX-009] El ajuste de stock y la actualización de la venta se hacen en una sola
     * transacción: si cualquiera de los dos falla, no se aplica ninguno. También se valida
     * que la cantidad sea mayor a cero y que la fecha tenga formato {@code yyyy-MM-dd}.
     *
     * @return {@code true} si la venta se actualizó.
     */
    public boolean editarVenta(Object id, String nuevoNombre, String nuevaCantidad, String nuevaFecha) {
        try {
            ensureConnection();
            int nuevaCant = Integer.parseInt(nuevaCantidad);
            if (nuevaCant <= 0) {
                JOptionPane.showMessageDialog(null, "La cantidad debe ser un número mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            try {
                LocalDate.parse(nuevaFecha, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException | NullPointerException e) {
                JOptionPane.showMessageDialog(null, "La fecha debe tener el formato YYYY-MM-DD y ser una fecha válida.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            // Obtener datos actuales de la venta incluyendo idProducto
            String selectSale = "SELECT idProducto, cantidad FROM historial_ventas WHERE id = ?";
            int idProducto;
            int oldQuantity;
            try (PreparedStatement ps = connection.prepareStatement(selectSale)) {
                ps.setObject(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idProducto = rs.getInt("idProducto");
                        oldQuantity = rs.getInt("cantidad");
                    } else {
                        JOptionPane.showMessageDialog(null, "No se encontró la venta con id: " + id, "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // Validar que el nuevo nombre coincida con el producto asociado mediante idProducto
            String selectProduct = "SELECT nombre FROM productos WHERE id = ?";
            try (PreparedStatement psProd = connection.prepareStatement(selectProduct)) {
                psProd.setInt(1, idProducto);
                try (ResultSet rsProd = psProd.executeQuery()) {
                    if (rsProd.next()) {
                        String productName = rsProd.getString("nombre");
                        if (!productName.equalsIgnoreCase(nuevoNombre)) {
                            JOptionPane.showMessageDialog(null, "El nombre ingresado no coincide con el producto asociado a esta venta.", "Error", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "No se encontró el producto asociado a esta venta.", "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            // Calcular la diferencia en cantidad
            int diff = nuevaCant - oldQuantity;

            // Si se aumenta la venta, se verifica que haya stock suficiente para ese producto en específico
            if (diff > 0) {
                String selectStock = "SELECT existencias FROM productos WHERE id = ?";
                try (PreparedStatement psStock = connection.prepareStatement(selectStock)) {
                    psStock.setInt(1, idProducto);
                    try (ResultSet rs = psStock.executeQuery()) {
                        if (rs.next()) {
                            int stock = rs.getInt("existencias");
                            if (stock < diff) {
                                JOptionPane.showMessageDialog(null, "No hay suficientes existencias para aumentar la venta.", "Error", JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "El producto asociado no existe en el inventario.", "Error", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
            }

            // Ajuste de stock + actualización de la venta: todo o nada
            final int productoVenta = idProducto;
            return enTransaccion(() -> {
                // La condición "existencias >= ?" evita dejar stock negativo aunque cambie entre lecturas
                String updateStock = "UPDATE productos SET existencias = existencias - ? WHERE id = ? AND existencias >= ?";
                try (PreparedStatement psUpdate = connection.prepareStatement(updateStock)) {
                    psUpdate.setInt(1, diff);
                    psUpdate.setInt(2, productoVenta);
                    psUpdate.setInt(3, Math.max(diff, 0));
                    if (psUpdate.executeUpdate() == 0) {
                        throw new SQLException("No se pudo actualizar el stock del producto " + productoVenta);
                    }
                }

                String sql = "UPDATE historial_ventas SET nombre = ?, cantidad = ?, fechaVenta = ? WHERE id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, nuevoNombre);
                    pstmt.setInt(2, nuevaCant);
                    pstmt.setString(3, nuevaFecha);
                    pstmt.setObject(4, id);
                    if (pstmt.executeUpdate() == 0) {
                        throw new SQLException("La venta " + id + " ya no existe");
                    }
                }
                return true;
            });
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al editar la venta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }




    /**
     * [FIX-009] Elimina una venta del historial y <b>regresa</b> la cantidad vendida al
     * stock del producto (se trata como cancelación). Ambas operaciones van en una sola
     * transacción.
     *
     * <p>Si el producto ya no existe en la tabla (ventas huérfanas de versiones anteriores
     * que borraban productos físicamente), solo se elimina el registro de la venta.
     *
     * @return {@code true} si la venta existía y se eliminó.
     */
    public boolean eliminarVenta(Object id) {
        try {
            ensureConnection();
            return enTransaccion(() -> {
                int idProducto;
                int cantidad;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT idProducto, cantidad FROM historial_ventas WHERE id = ?")) {
                    ps.setObject(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        idProducto = rs.getInt("idProducto");
                        cantidad = rs.getInt("cantidad");
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE productos SET existencias = existencias + ? WHERE id = ?")) {
                    ps.setInt(1, cantidad);
                    ps.setInt(2, idProducto);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM historial_ventas WHERE id = ?")) {
                    ps.setObject(1, id);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("La venta " + id + " ya no existe");
                    }
                }
                return true;
            });
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> obtenerHistorialVentas() {
        List<Object[]> ventas = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, idProducto, nombre, cantidad, fechaVenta FROM historial_ventas ORDER BY id ASC")) {

            while (rs.next()) {
                Object idVenta = rs.getInt("id");           // 🟢 ID de la venta
                Object idProducto = rs.getInt("idProducto"); // 🟢 ID del producto
                String nombre = rs.getString("nombre");     // 🟢 Nombre del medicamento
                int cantidad = rs.getInt("cantidad");       // 🟢 Cantidad vendida
                String fecha = rs.getString("fechaVenta");  // 🟢 Fecha de la venta

                System.out.println("BD -> " + idVenta + ", " + idProducto + ", " + nombre + ", " + cantidad + ", " + fecha); // Debug

                if (idVenta != null && idProducto != null && nombre != null && cantidad > 0 && fecha != null) {
                    ventas.add(new Object[]{idVenta, idProducto, nombre, cantidad, fecha}); // ✅ Ahora aseguramos que devuelve 5 valores
                } else {
                    System.err.println("Error: Registro de venta con datos incompletos -> " +
                            "[" + idVenta + ", " + idProducto + ", " + nombre + ", " + cantidad + ", " + fecha + "]");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ventas;
    }







    /**
     * [MR-005 – OPC-A] Genera un reporte HTML del inventario completo de productos y lo
     * guarda en {@code fileToSave}. Sustituye al anterior {@code exportarInventarioCSV}.
     * El archivo incluye CSS embebido y encabezado de veterinaria si existe configuración.
     *
     * <p>Columnas del reporte: ID, Nombre, Existencias, Lote, Caducidad, Fecha Entrada.
     *
     * <p>[FIX-009] Solo productos activos; sin diálogos (la vista informa); texto escapado.
     *
     * @param fileToSave Ruta destino del archivo {@code .html} a generar.
     * @return {@code true} si el archivo se generó.
     */
    public boolean exportarInventarioHTML(File fileToSave) {
        try (Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nombre, existencias, lote, caducidad, fechaEntrada FROM productos WHERE activo = 1")) {
            StringBuilder html = buildHtmlHeader("Reporte de Inventario");
            html.append("<table><thead><tr>")
                .append("<th>ID</th><th>Nombre</th><th>Existencias</th><th>Lote</th><th>Caducidad</th><th>Fecha Entrada</th>")
                .append("</tr></thead><tbody>\n");
            while (rs.next()) {
                html.append("<tr>")
                    .append("<td>").append(rs.getInt("id")).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("nombre"))).append("</td>")
                    .append("<td>").append(rs.getInt("existencias")).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("lote"))).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("caducidad"))).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("fechaEntrada"))).append("</td>")
                    .append("</tr>\n");
            }
            html.append("</tbody></table></body></html>");
            escribirArchivo(fileToSave, html.toString());
            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    /**
     * Retorna los productos cuya fecha de expiración está dentro de los próximos
     * {@code diasUmbral} días.
     *
     * <p>[MR-003 OPC-A] La fecha de expiración es el primer día del mes
     * <em>siguiente</em> al indicado en {@code caducidad} (yyyy-MM). Así, un producto
     * con caducidad igual al mes actual aparece como próximo a caducar (no como caducado).
     *
     * @param diasUmbral Número máximo de días restantes para considerar un producto próximo.
     * @return Lista de arreglos {@code {id, nombre, existencias, lote, caducidad, fechaEntrada}}.
     */
    public List<Object[]> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        List<Object[]> proximos = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + COLUMNAS_PRODUCTO + " FROM productos WHERE activo = 1")) {

            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                String caducidadStr = rs.getString("caducidad"); // "yyyy-MM"
                // [MR-003 OPC-A] Registros con caducidad corrupta se omiten (estado INVALIDA).
                // [FIX-009] El día en que expira cuenta solo como caducado, no también como próximo.
                if (ReglasCaducidad.estado(caducidadStr, hoy, diasUmbral) == ReglasCaducidad.Estado.PROXIMO) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    int existencias = rs.getInt("existencias");
                    String lote = rs.getString("lote");
                    String fechaEntrada = rs.getString("fechaEntrada");
                    proximos.add(new Object[]{id, nombre, existencias, lote, caducidadStr, fechaEntrada});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return proximos;
    }




    /**
     * Retorna los productos cuya fecha de expiración ya ha pasado.
     *
     * <p>[MR-003 OPC-A] La fecha de expiración es el primer día del mes
     * <em>siguiente</em> al indicado en {@code caducidad} (yyyy-MM). Un producto
     * se considera caducado cuando {@code LocalDate.now() >= cadYM.plusMonths(1).atDay(1)}.
     *
     * @return Lista de arreglos {@code {id, nombre, existencias, lote, caducidad, fechaEntrada}}.
     */
    public List<Object[]> obtenerMedicamentosCaducados() {
        List<Object[]> caducados = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + COLUMNAS_PRODUCTO + " FROM productos WHERE activo = 1")) {

            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                String caducidadStr = rs.getString("caducidad"); // "yyyy-MM"
                // [MR-003 OPC-A] Registros con caducidad corrupta se omiten (estado INVALIDA).
                if (ReglasCaducidad.estado(caducidadStr, hoy, ReglasCaducidad.DIAS_ALERTA) == ReglasCaducidad.Estado.CADUCADO) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    int existencias = rs.getInt("existencias");
                    String lote = rs.getString("lote");
                    String fechaEntrada = rs.getString("fechaEntrada");
                    caducados.add(new Object[]{id, nombre, existencias, lote, caducidadStr, fechaEntrada});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return caducados;
    }


    /**
     * Determina si un medicamento está caducado.
     * Un medicamento se considera caducado únicamente cuando su mes de caducidad ya pasó
     * (es decir, el mes y año actuales son posteriores al mes de caducidad registrado).
     *
     * @param cad mes de caducidad del medicamento en formato YearMonth
     * @return {@code true} si el mes actual es posterior al mes de caducidad; {@code false} en caso contrario
     */
    private boolean estaCaducado(YearMonth cad) {
        return ReglasCaducidad.estaCaducado(cad, LocalDate.now());
    }

    /**
     * Registra el apartado de un producto en la base de datos.
     * Antes de ejecutar el UPDATE, valida que el producto no esté caducado y que tenga
     * existencias disponibles. Si alguna validación falla, muestra un mensaje de error
     * mediante JOptionPane y retorna {@code false} sin modificar la base de datos.
     *
     * @param id           identificador del producto a apartar
     * @param fechaSeparado fecha en que se registra el apartado (formato "yyyy-MM-dd")
     * @return {@code true} si el apartado se registró correctamente; {@code false} si el producto
     *         está caducado, no tiene existencias, no existe o ocurre un error de base de datos
     */
    public boolean separarProducto(int id, String fechaSeparado) {
        try {
            ensureConnection();
            String selectSql = "SELECT nombre, caducidad, existencias FROM productos WHERE id = ? AND activo = 1";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setInt(1, id);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) return false;
                    String caducidadStr = rs.getString("caducidad");
                    int existencias = rs.getInt("existencias");
                    // [MR-003 OPC-A] Parseo seguro: ante caducidad corrupta, no se aparta.
                    YearMonth caducidad = parsearCaducidad(caducidadStr);
                    if (caducidad == null) {
                        JOptionPane.showMessageDialog(null,
                            "No se puede registrar el apartado: la caducidad del producto no tiene un formato válido",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    if (estaCaducado(caducidad)) {
                        JOptionPane.showMessageDialog(null,
                            "No se puede registrar el apartado: el medicamento está caducado",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    if (existencias <= 0) {
                        JOptionPane.showMessageDialog(null,
                            "No se puede registrar el apartado: el medicamento no tiene existencias disponibles",
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
            String updateSql = "UPDATE productos SET fecha_separado = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                pstmt.setString(1, fechaSeparado);
                pstmt.setInt(2, id);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public List<Object[]> obtenerProductosApartados() {
        List<Object[]> apartados = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT " + COLUMNAS_PRODUCTO + " FROM productos WHERE activo = 1 AND fecha_separado IS NOT NULL AND fecha_separado <> ''")) {
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                apartados.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apartados;
    }



    /**
     * [MR-005 – OPC-A] Genera un reporte HTML de los productos apartados y lo guarda en
     * {@code fileToSave}. Sustituye al anterior {@code exportarApartadosCSV}. Solo incluye
     * productos cuyo campo {@code fecha_separado} no sea {@code NULL} ni vacío.
     * El archivo incluye CSS embebido y encabezado de veterinaria si existe configuración.
     *
     * <p>Columnas del reporte: ID, Nombre, Existencias, Lote, Caducidad, Fecha Entrada,
     * Fecha Apartado.
     *
     * <p>[FIX-009] Solo productos activos; sin diálogos (la vista informa); texto escapado.
     *
     * @param fileToSave Ruta destino del archivo {@code .html} a generar.
     * @return {@code true} si el archivo se generó.
     */
    public boolean exportarApartadosHTML(File fileToSave) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nombre, existencias, lote, caducidad, fechaEntrada, fecha_separado FROM productos WHERE activo = 1 AND fecha_separado IS NOT NULL AND fecha_separado <> ''")) {
            StringBuilder html = buildHtmlHeader("Reporte de Apartados");
            html.append("<table><thead><tr>")
                .append("<th>ID</th><th>Nombre</th><th>Existencias</th><th>Lote</th><th>Caducidad</th><th>Fecha Entrada</th><th>Fecha Apartado</th>")
                .append("</tr></thead><tbody>\n");
            while (rs.next()) {
                html.append("<tr>")
                    .append("<td>").append(rs.getInt("id")).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("nombre"))).append("</td>")
                    .append("<td>").append(rs.getInt("existencias")).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("lote"))).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("caducidad"))).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("fechaEntrada"))).append("</td>")
                    .append("<td>").append(escaparHtml(rs.getString("fecha_separado"))).append("</td>")
                    .append("</tr>\n");
            }
            html.append("</tbody></table></body></html>");
            escribirArchivo(fileToSave, html.toString());
            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * [FIX-009] Resuelve un nombre al ID del mejor lote (ver
     * {@link #buscarProductoPorIdONombre(String)}).
     *
     * @return el ID, o {@code -1} si no hay ningún producto activo con ese nombre.
     */
    public int obtenerProductoIdPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return -1;
        }
        try {
            ensureConnection();
            Object[] producto = buscarMejorLotePorNombre(nombre.trim());
            if (producto != null) {
                return (int) producto[0];
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Retorna -1 si no se encuentra el producto.
    }

    /**
     * [MR-002 – OPC-A] Busca un producto por ID numérico exacto o por nombre completo.
     *
     * <p>Estrategia de búsqueda:
     * <ol>
     *   <li>Si {@code dato} es un entero, busca el producto activo con ese ID
     *       (el usuario eligió un lote concreto).</li>
     *   <li>Si no es número o no hay resultado, busca por nombre completo
     *       (sin comodines). [FIX-009] La comparación ignora mayúsculas/minúsculas y,
     *       si hay varios lotes con el mismo nombre, devuelve el lote <b>vendible</b>
     *       (con existencias y sin caducar) que caduca primero (FEFO). Si ningún lote es
     *       vendible, devuelve el primero que coincida para que la venta o el apartado
     *       muestre el motivo concreto (caducado o sin existencias).</li>
     * </ol>
     *
     * @param dato Cadena ingresada por el usuario; puede ser un ID numérico o
     *             el nombre completo del medicamento.
     * @return {@code Object[]{id, nombre}} si se encuentra el producto,
     *         {@code null} si no existe ninguna coincidencia.
     */
    public Object[] buscarProductoPorIdONombre(String dato) {
        if (dato == null || dato.trim().isEmpty()) {
            return null;
        }
        String texto = dato.trim();
        try {
            ensureConnection();
            try {
                int id = Integer.parseInt(texto);
                String sql = "SELECT id, nombre FROM productos WHERE id = ? AND activo = 1";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return new Object[]{rs.getInt("id"), rs.getString("nombre")};
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // El dato no es un número; buscar por nombre
            }
            return buscarMejorLotePorNombre(texto);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * [FIX-009] Entre los productos activos cuyo nombre coincide (sin distinguir
     * mayúsculas), elige el lote vendible que caduca primero; si ninguno es vendible,
     * el primero que coincida.
     *
     * <p>La comparación se hace en Java porque {@code lower()} de SQLite solo maneja ASCII
     * (no iguala "Á" con "á").
     */
    private Object[] buscarMejorLotePorNombre(String nombre) throws SQLException {
        String sql = "SELECT id, nombre, existencias, caducidad FROM productos WHERE activo = 1 ORDER BY id";
        LocalDate hoy = LocalDate.now();
        Object[] mejor = null;
        YearMonth caducidadMejor = null;
        Object[] primeraCoincidencia = null;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String nombreProducto = rs.getString("nombre");
                if (nombreProducto == null || !nombreProducto.trim().equalsIgnoreCase(nombre)) {
                    continue;
                }
                Object[] fila = {rs.getInt("id"), nombreProducto};
                if (primeraCoincidencia == null) {
                    primeraCoincidencia = fila;
                }
                YearMonth caducidad = ReglasCaducidad.parsear(rs.getString("caducidad"));
                boolean vendible = rs.getInt("existencias") > 0
                        && caducidad != null
                        && !ReglasCaducidad.estaCaducado(caducidad, hoy);
                if (vendible && (mejor == null || caducidad.isBefore(caducidadMejor))) {
                    mejor = fila;
                    caducidadMejor = caducidad;
                }
            }
        }
        return mejor != null ? mejor : primeraCoincidencia;
    }



    public boolean eliminarApartado(int id) {
        try {
            ensureConnection();
            String sql = "UPDATE productos SET fecha_separado = NULL WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }



    public boolean editarApartado(int id, String nuevaFechaApartado) {
        try {
            ensureConnection();
            String sql = "UPDATE productos SET fecha_separado = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, nuevaFechaApartado);
                pstmt.setInt(2, id);
                int affected = pstmt.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
