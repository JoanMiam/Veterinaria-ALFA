package model;

import java.io.File;
import java.io.FileWriter;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class InventarioDAO {

    private Connection connection;
    private String dbUrl;

    public InventarioDAO(String dbUrl) {
        this.dbUrl = dbUrl;
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(dbUrl);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }


    // Constructor para pruebas (acepta conexión existente)
    public InventarioDAO(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void ensureConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connection = DriverManager.getConnection(this.dbUrl);
        }
    }

    public InventarioDAO() {
        // 1. Ruta relativa al directorio actual
        String carpetaBD = "./db"; // subcarpeta "db" junto al .exe/.jar

        // 2. Crear la carpeta si no existe
        File folderFile = new File(carpetaBD);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }

        // 3. Construir la URL de la base de datos
        this.dbUrl = "jdbc:sqlite:" + carpetaBD + "/baseDeDatosInventario.db";

        // 4. Conectar
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(dbUrl);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }


    public void crearBaseDeDatos() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS productos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT, " +
                    "existencias INTEGER, " +
                    "lote TEXT, " +
                    "caducidad TEXT, " +
                    "fechaEntrada TEXT, " +
                    "fecha_separado TEXT)"); // NUEVA COLUMNA
            stmt.execute("CREATE TABLE IF NOT EXISTS historial_ventas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "idProducto INTEGER, " +
                    "nombre TEXT, " +
                    "cantidad INTEGER, " +
                    "fechaVenta TEXT, " +
                    "FOREIGN KEY (idProducto) REFERENCES productos(id) ON DELETE CASCADE)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public Object[][] obtenerProductos() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {
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





    public boolean eliminarProducto(int id) {
        try {
            ensureConnection();
            String sql = "DELETE FROM productos WHERE id = ?";
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

            String sql = "UPDATE productos SET nombre = ?, existencias = ?, lote = ?, caducidad = ?, fechaEntrada = ? WHERE id = ?";
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



    public void exportarCSV(File fileToSave) {
        try (FileWriter writer = new FileWriter(fileToSave);
             Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre, cantidad, fechaVenta FROM historial_ventas")) {
            writer.append("Nombre, Cantidad Vendida, Fecha de Venta\n");
            while (rs.next()) {
                writer.append(rs.getString("nombre")).append(",");
                writer.append(String.valueOf(rs.getInt("cantidad"))).append(",");
                writer.append(rs.getString("fechaVenta") != null ? rs.getString("fechaVenta") : "N/A").append("\n");
            }
            writer.flush();
            JOptionPane.showMessageDialog(null, "CSV exportado con éxito en:\n" + fileToSave.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al exportar CSV: " + e.getMessage());
        }
    }

    private boolean validarFechaCaducidad(String fechaCaducidad) {
        // Validar con expresión regular: 4 dígitos de año, guión, 2 dígitos de mes
        if (!fechaCaducidad.matches("\\d{4}-\\d{2}")) {
            return false;
        }
        // O adicionalmente intentar parsear con YearMonth
        try {
            DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            YearMonth.parse(fechaCaducidad, ymFormatter);
            return true; // si parsea, es válido
        } catch (DateTimeParseException e) {
            return false;
        }
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

            // Consulta el producto por su ID
            String selectQuery = "SELECT nombre, existencias, caducidad FROM productos WHERE id = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, id);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "El producto con ID " + id + " no existe.", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                // Recupera el nombre real y el stock actual
                String nombreReal = rs.getString("nombre");
                int existenciasActuales = rs.getInt("existencias");
                String caducidad = rs.getString("caducidad");

                // Compara el nombre ingresado con el registrado
                if (!nombreReal.equalsIgnoreCase(nombreIngresado)) {
                    JOptionPane.showMessageDialog(null, "El nombre ingresado no coincide con el producto registrado para el ID " + id + ".", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                if (cantidadVendida > existenciasActuales) {
                    JOptionPane.showMessageDialog(null, "No hay suficientes existencias.", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                // [MR-003 OPC-A] Validar que el producto no esté caducado.
                // Un producto caduca al inicio del mes siguiente al indicado en caducidad.
                YearMonth cadYM = YearMonth.parse(caducidad, DateTimeFormatter.ofPattern("yyyy-MM"));
                LocalDate fechaExpiracion = cadYM.plusMonths(1).atDay(1);
                if (!LocalDate.now().isBefore(fechaExpiracion)) {
                    JOptionPane.showMessageDialog(null, "No se puede vender un producto caducado.", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                // Inicia una transacción
                connection.setAutoCommit(false);

                // Descuenta del inventario
                String updateQuery = "UPDATE productos SET existencias = existencias - ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, cantidadVendida);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();
                }

                // Registrar la venta con la fecha actual
                String fechaVenta = LocalDate.now().toString(); // Asegurar que siempre tenga una fecha válida
                String insertHistorial = "INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertHistorial)) {
                    insertStmt.setInt(1, id);
                    insertStmt.setString(2, nombreReal);
                    insertStmt.setInt(3, cantidadVendida);
                    insertStmt.setString(4, fechaVenta);  // Se asigna la fecha actual
                    insertStmt.executeUpdate();
                }

                connection.commit();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
        return false;
    }






    public boolean editarVenta(Object id, String nuevoNombre, String nuevaCantidad, String nuevaFecha) {
        try {
            int nuevaCant = Integer.parseInt(nuevaCantidad);

            // Obtener datos actuales de la venta incluyendo idProducto
            String selectSale = "SELECT idProducto, nombre, cantidad FROM historial_ventas WHERE id = ?";
            int idProducto = -1;
            String oldName = null;
            int oldQuantity = 0;
            try (PreparedStatement ps = connection.prepareStatement(selectSale)) {
                ps.setObject(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        idProducto = rs.getInt("idProducto");
                        oldName = rs.getString("nombre");
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

            // Actualizar el stock utilizando el idProducto
            String updateStock = "UPDATE productos SET existencias = existencias - ? WHERE id = ?";
            try (PreparedStatement psUpdate = connection.prepareStatement(updateStock)) {
                psUpdate.setInt(1, diff);
                psUpdate.setInt(2, idProducto);
                int affected = psUpdate.executeUpdate();
                if (affected == 0) {
                    JOptionPane.showMessageDialog(null, "No se pudo actualizar el stock del producto.", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

            // Actualizar el registro de la venta
            String sql = "UPDATE historial_ventas SET nombre = ?, cantidad = ?, fechaVenta = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, nuevoNombre);
                pstmt.setInt(2, nuevaCant);
                pstmt.setString(3, nuevaFecha);
                pstmt.setObject(4, id);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al editar la venta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }




    public boolean eliminarVenta(Object id) {
        try {
            String sql = "DELETE FROM historial_ventas WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setObject(1, id);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
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







    public void exportarInventarioCSV(File fileToSave) {
        try (FileWriter writer = new FileWriter(fileToSave);
             Statement stmt = this.connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nombre, existencias, lote, caducidad, fechaEntrada FROM productos")) {

            // Escribe la cabecera del CSV para el inventario
            writer.append("ID,Nombre,Existencias,Lote,Caducidad,Fecha Entrada\n");

            // Itera sobre cada fila y escribe los datos
            while (rs.next()) {
                writer.append(String.valueOf(rs.getInt("id"))).append(",");
                writer.append(rs.getString("nombre")).append(",");
                writer.append(String.valueOf(rs.getInt("existencias"))).append(",");
                writer.append(rs.getString("lote")).append(",");
                writer.append(rs.getString("caducidad")).append(",");
                writer.append(rs.getString("fechaEntrada")).append("\n");
            }
            writer.flush();
            JOptionPane.showMessageDialog(null, "Inventario exportado con éxito en:\n" + fileToSave.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al exportar el inventario: " + e.getMessage());
        }
    }



    public List<Object[]> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        List<Object[]> proximos = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {

            DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                String caducidadStr = rs.getString("caducidad"); // "yyyy-MM"
                // Parseamos a YearMonth
                YearMonth cadYM = YearMonth.parse(caducidadStr, ymFormatter);

                // Tomamos el primer día de ese mes para calcular días restantes
                LocalDate primerDiaCaducidad = cadYM.atDay(1);
                long diasRestantes = ChronoUnit.DAYS.between(hoy, primerDiaCaducidad);

                if (diasRestantes >= 0 && diasRestantes <= diasUmbral) {
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




    public List<Object[]> obtenerMedicamentosCaducados() {
        List<Object[]> caducados = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {

            DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                String caducidadStr = rs.getString("caducidad"); // "yyyy-MM"
                YearMonth cadYM = YearMonth.parse(caducidadStr, ymFormatter);
                LocalDate primerDiaCaducidad = cadYM.atDay(1);

                if (primerDiaCaducidad.isBefore(hoy)) {
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





    public boolean separarProducto(int id, String fechaSeparado) {
        try {
            ensureConnection();
            String sql = "UPDATE productos SET fecha_separado = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
             ResultSet rs = stmt.executeQuery("SELECT * FROM productos WHERE fecha_separado IS NOT NULL AND fecha_separado <> ''")) {
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



    public void exportarApartadosCSV(File fileToSave) {
        try (FileWriter writer = new FileWriter(fileToSave);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, nombre, existencias, lote, caducidad, fechaEntrada, fecha_separado FROM productos WHERE fecha_separado IS NOT NULL AND fecha_separado <> ''")) {

            writer.append("ID,Nombre,Existencias,Lote,Caducidad,Fecha Entrada,Fecha Apartado\n");
            while (rs.next()) {
                writer.append(String.valueOf(rs.getInt("id"))).append(",");
                writer.append(rs.getString("nombre")).append(",");
                writer.append(String.valueOf(rs.getInt("existencias"))).append(",");
                writer.append(rs.getString("lote")).append(",");
                writer.append(rs.getString("caducidad")).append(",");
                writer.append(rs.getString("fechaEntrada")).append(",");
                writer.append(rs.getString("fecha_separado")).append("\n");
            }
            writer.flush();
            JOptionPane.showMessageDialog(null, "CSV de Apartados exportado con éxito en:\n" + fileToSave.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al exportar CSV de Apartados: " + e.getMessage());
        }
    }


    public int obtenerProductoIdPorNombre(String nombre) {
        try {
            ensureConnection();
            String sql = "SELECT id FROM productos WHERE nombre = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, nombre);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Retorna -1 si no se encuentra el producto.
    }

    /**
     * [MR-002 – OPC-A] Busca un producto por ID numérico exacto o por nombre exacto.
     *
     * <p>Estrategia de búsqueda:
     * <ol>
     *   <li>Intenta parsear {@code dato} como entero; si lo logra, ejecuta
     *       {@code SELECT id, nombre FROM productos WHERE id = ?}.</li>
     *   <li>Si el parseo falla ({@link NumberFormatException}) o no hay resultado,
     *       ejecuta {@code SELECT id, nombre FROM productos WHERE nombre = ?}
     *       (coincidencia exacta, sin comodines).</li>
     * </ol>
     *
     * @param dato Cadena ingresada por el usuario; puede ser un ID numérico o
     *             el nombre exacto del medicamento.
     * @return {@code Object[]{id, nombre}} si se encuentra el producto,
     *         {@code null} si no existe ninguna coincidencia.
     */
    public Object[] buscarProductoPorIdONombre(String dato) {
        try {
            ensureConnection();
            try {
                int id = Integer.parseInt(dato.trim());
                String sql = "SELECT id, nombre FROM productos WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return new Object[]{rs.getInt("id"), rs.getString("nombre")};
                    }
                }
            } catch (NumberFormatException e) {
                // El dato no es un número; buscar por nombre exacto
            }
            String sql = "SELECT id, nombre FROM productos WHERE nombre = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, dato.trim());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new Object[]{rs.getInt("id"), rs.getString("nombre")};
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
