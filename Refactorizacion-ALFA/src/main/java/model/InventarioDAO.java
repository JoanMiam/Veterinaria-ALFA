package model;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import model.entities.Producto;
import model.entities.Venta;

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
        String carpetaBD = "./db"; 
        File folderFile = new File(carpetaBD);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }

        this.dbUrl = "jdbc:sqlite:" + carpetaBD + "/baseDeDatosInventario.db";

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
                    "fecha_separado TEXT"); 
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

    public List<Producto> obtenerProductos() {
        List<Producto> productos = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {
            while (rs.next()) {
                productos.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productos;
    }

    public boolean agregarProducto(String nombre, String existencias, String lote, String caducidad) {
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
                JOptionPane.showMessageDialog(null, "Las existencias no pueden ser negativas.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String fechaEntrada = LocalDate.now().toString();
            String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
                pstmt.setInt(2, existenciasInt);
                pstmt.setString(3, lote);
                pstmt.setString(4, caducidad); 
                pstmt.setString(5, fechaEntrada);
                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Las existencias deben ser un número válido.", "Error",
                    JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(null, "Las existencias no pueden ser negativas.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            String sql = "UPDATE productos SET nombre = ?, existencias = ?, lote = ?, caducidad = ?, fechaEntrada = ? WHERE id = ?";
            try (PreparedStatement pstmt = this.connection.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
                pstmt.setInt(2, existenciasInt);
                pstmt.setString(3, lote);
                pstmt.setString(4, caducidad); 
                pstmt.setString(5, fechaEntrada);
                pstmt.setInt(6, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Las existencias deben ser un número válido.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al actualizar el producto: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
        if (!fechaCaducidad.matches("\\d{4}-\\d{2}")) {
            return false;
        }
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

    private Producto mapResultSetToProducto(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("existencias"),
                rs.getString("lote"),
                rs.getString("caducidad"),
                rs.getString("fechaEntrada"),
                rs.getString("fecha_separado"));
    }

    private Venta mapResultSetToVenta(ResultSet rs) throws SQLException {
        return new Venta(
                rs.getInt("id"),
                rs.getInt("idProducto"),
                rs.getString("nombre"),
                rs.getInt("cantidad"),
                rs.getString("fechaVenta"));
    }

    public boolean registrarVenta(int id, String nombreIngresado, int cantidadVendida) {
        try {
            ensureConnection();

            String selectQuery = "SELECT nombre, existencias FROM productos WHERE id = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, id);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(null, "El producto con ID " + id + " no existe.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                String nombreReal = rs.getString("nombre");
                int existenciasActuales = rs.getInt("existencias");

                if (!nombreReal.equalsIgnoreCase(nombreIngresado)) {
                    JOptionPane.showMessageDialog(null,
                            "El nombre ingresado no coincide con el producto registrado para el ID " + id + ".",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                if (cantidadVendida > existenciasActuales) {
                    JOptionPane.showMessageDialog(null, "No hay suficientes existencias.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                connection.setAutoCommit(false);

                String updateQuery = "UPDATE productos SET existencias = existencias - ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, cantidadVendida);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();
                }

                String fechaVenta = LocalDate.now().toString(); 
                String insertHistorial = "INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertHistorial)) {
                    insertStmt.setInt(1, id);
                    insertStmt.setString(2, nombreReal);
                    insertStmt.setInt(3, cantidadVendida);
                    insertStmt.setString(4, fechaVenta); 
                    insertStmt.executeUpdate();
                }

                connection.commit();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public boolean editarVenta(Object id, String nuevoNombre, String nuevaCantidad, String nuevaFecha) {
        try {
            int nuevaCant = Integer.parseInt(nuevaCantidad);

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
                        JOptionPane.showMessageDialog(null, "No se encontró la venta con id: " + id, "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            String selectProduct = "SELECT nombre FROM productos WHERE id = ?";
            try (PreparedStatement psProd = connection.prepareStatement(selectProduct)) {
                psProd.setInt(1, idProducto);
                try (ResultSet rsProd = psProd.executeQuery()) {
                    if (rsProd.next()) {
                        String productName = rsProd.getString("nombre");
                        if (!productName.equalsIgnoreCase(nuevoNombre)) {
                            JOptionPane.showMessageDialog(null,
                                    "El nombre ingresado no coincide con el producto asociado a esta venta.", "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "No se encontró el producto asociado a esta venta.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }

            int diff = nuevaCant - oldQuantity;

            if (diff > 0) {
                String selectStock = "SELECT existencias FROM productos WHERE id = ?";
                try (PreparedStatement psStock = connection.prepareStatement(selectStock)) {
                    psStock.setInt(1, idProducto);
                    try (ResultSet rs = psStock.executeQuery()) {
                        if (rs.next()) {
                            int stock = rs.getInt("existencias");
                            if (stock < diff) {
                                JOptionPane.showMessageDialog(null,
                                        "No hay suficientes existencias para aumentar la venta.", "Error",
                                        JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "El producto asociado no existe en el inventario.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                }
            }

            String updateStock = "UPDATE productos SET existencias = existencias - ? WHERE id = ?";
            try (PreparedStatement psUpdate = connection.prepareStatement(updateStock)) {
                psUpdate.setInt(1, diff);
                psUpdate.setInt(2, idProducto);
                int affected = psUpdate.executeUpdate();
                if (affected == 0) {
                    JOptionPane.showMessageDialog(null, "No se pudo actualizar el stock del producto.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }

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
            JOptionPane.showMessageDialog(null, "Error al editar la venta: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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

    public List<Venta> obtenerHistorialVentas() {
        List<Venta> ventas = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id, idProducto, nombre, cantidad, fechaVenta FROM historial_ventas ORDER BY id ASC")) {

            while (rs.next()) {
                ventas.add(mapResultSetToVenta(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ventas;
    }

    public void exportarInventarioCSV(File fileToSave) {
        try (FileWriter writer = new FileWriter(fileToSave);
                Statement stmt = this.connection.createStatement();
                ResultSet rs = stmt
                        .executeQuery("SELECT id, nombre, existencias, lote, caducidad, fechaEntrada FROM productos")) {

            writer.append("ID,Nombre,Existencias,Lote,Caducidad,Fecha Entrada\n");

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

    public List<Producto> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        List<Producto> proximos = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {

            DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                String caducidadStr = rs.getString("caducidad");
                YearMonth cadYM = YearMonth.parse(caducidadStr, ymFormatter);
                LocalDate primerDiaCaducidad = cadYM.atDay(1);
                long diasRestantes = ChronoUnit.DAYS.between(hoy, primerDiaCaducidad);

                if (diasRestantes >= 0 && diasRestantes <= diasUmbral) {
                    proximos.add(mapResultSetToProducto(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return proximos;
    }

    public List<Producto> obtenerMedicamentosCaducados() {
        List<Producto> caducados = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM productos")) {

            DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDate hoy = LocalDate.now();

            while (rs.next()) {
                String caducidadStr = rs.getString("caducidad");
                YearMonth cadYM = YearMonth.parse(caducidadStr, ymFormatter);
                LocalDate primerDiaCaducidad = cadYM.atDay(1);

                if (primerDiaCaducidad.isBefore(hoy)) {
                    caducados.add(mapResultSetToProducto(rs));
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

    public List<Producto> obtenerProductosApartados() {
        List<Producto> apartados = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM productos WHERE fecha_separado IS NOT NULL AND fecha_separado <> ''")) {
            while (rs.next()) {
                apartados.add(mapResultSetToProducto(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apartados;
    }

    public void exportarApartadosCSV(File fileToSave) {
        try (FileWriter writer = new FileWriter(fileToSave);
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id, nombre, existencias, lote, caducidad, fechaEntrada, fecha_separado FROM productos WHERE fecha_separado IS NOT NULL AND fecha_separado <> ''")) {

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
            JOptionPane.showMessageDialog(null,
                    "CSV de Apartados exportado con éxito en:\n" + fileToSave.getAbsolutePath());
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
        return -1; 
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
