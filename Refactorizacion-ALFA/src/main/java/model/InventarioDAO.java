package model;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import util.RutasApp;

/**
 * Acceso a datos del inventario en SQLite.
 *
 * <p>[MR-010] Esta clase ya no muestra diálogos ni conoce Swing:
 * <ul>
 *   <li>Si una regla de negocio no se cumple (producto caducado, sin existencias, dato
 *       obligatorio vacío...), lanza {@link OperacionRechazadaException} con un mensaje
 *       listo para mostrar al usuario.</li>
 *   <li>Si la base de datos falla, lanza {@link ErrorDatosException}.</li>
 * </ul>
 * Los datos se devuelven como records ({@link Producto}, {@link Venta},
 * {@link ConfiguracionVeterinaria}) en lugar de {@code Object[]}.
 *
 * <p>Reglas vigentes (MR-003, MR-008, FIX-009):
 * <ul>
 *   <li>Caducidad {@code yyyy-MM}; el producto expira el 1.º del mes siguiente ({@link ReglasCaducidad}).</li>
 *   <li>Eliminar un producto es borrado lógico ({@code activo = 0}); su historial se conserva.</li>
 *   <li>Eliminar una venta regresa la cantidad al stock.</li>
 *   <li>Las operaciones que tocan stock y ventas son transaccionales.</li>
 * </ul>
 */
public class InventarioDAO {

    /** Columnas de producto, en el orden en que se muestran en las tablas. */
    private static final String COLUMNAS_PRODUCTO =
            "id, nombre, existencias, lote, caducidad, fechaEntrada, fecha_separado";

    private final Connection connection;

    /**
     * [FIX-009] Abre la BD en {@code <carpeta del ejecutable>/db/baseDeDatosInventario.db}
     * (ver {@link RutasApp}) y aplica las migraciones pendientes.
     *
     * @throws IllegalStateException si no se puede preparar la carpeta, abrir la BD o migrarla.
     */
    public InventarioDAO() {
        this(abrirConexion(urlPorDefecto()));
    }

    /**
     * Abre (o crea) la base de datos en {@code dbUrl} y aplica las migraciones pendientes.
     *
     * @throws IllegalStateException si no se puede abrir la BD o migrarla.
     */
    public InventarioDAO(String dbUrl) {
        this(abrirConexion(dbUrl));
    }

    /**
     * Usa una conexión existente (pruebas) y aplica las migraciones pendientes.
     *
     * @throws IllegalStateException si no se puede migrar el esquema.
     */
    public InventarioDAO(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
        try {
            EsquemaBD.actualizar(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar el esquema de la base de datos: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private static String urlPorDefecto() {
        try {
            return "jdbc:sqlite:" + RutasApp.prepararBaseDatos();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo preparar la carpeta de la base de datos ("
                    + RutasApp.archivoBaseDatos() + "): " + e.getMessage(), e);
        }
    }

    private static Connection abrirConexion(String url) {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("No se pudo abrir la base de datos (" + url + "): " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Productos
    // =========================================================================

    /** @return productos activos (los eliminados quedan ocultos). */
    public List<Producto> obtenerProductos() {
        return consultarProductos("WHERE activo = 1");
    }

    /**
     * Registra un producto (lote) con fecha de entrada de hoy.
     *
     * @return el producto creado.
     * @throws OperacionRechazadaException si falta un dato o las existencias son negativas.
     */
    public Producto agregarProducto(String nombre, int existencias, String lote, YearMonth caducidad)
            throws OperacionRechazadaException {
        String nombreLimpio = requerido(nombre, "El nombre del medicamento es obligatorio.");
        String loteLimpio = requerido(lote, "El lote es obligatorio.");
        validarExistencias(existencias);
        if (caducidad == null) {
            throw rechazo("La fecha de caducidad es obligatoria.");
        }
        String fechaEntrada = LocalDate.now().toString();
        String sql = "INSERT INTO productos (nombre, existencias, lote, caducidad, fechaEntrada) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreLimpio);
            ps.setInt(2, existencias);
            ps.setString(3, loteLimpio);
            ps.setString(4, caducidad.toString());
            ps.setString(5, fechaEntrada);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : -1;
                return new Producto(id, nombreLimpio, existencias, loteLimpio, caducidad.toString(), fechaEntrada, null);
            }
        } catch (SQLException e) {
            throw errorDatos("agregar el producto", e);
        }
    }

    /**
     * Actualiza los datos de un producto activo.
     *
     * @throws OperacionRechazadaException si falta un dato, las existencias son negativas
     *                                     o el producto ya no existe.
     */
    public void editarProducto(int id, String nombre, int existencias, String lote,
                               YearMonth caducidad, LocalDate fechaEntrada) throws OperacionRechazadaException {
        String nombreLimpio = requerido(nombre, "El nombre del medicamento es obligatorio.");
        String loteLimpio = requerido(lote, "El lote es obligatorio.");
        validarExistencias(existencias);
        if (caducidad == null) {
            throw rechazo("La fecha de caducidad es obligatoria.");
        }
        if (fechaEntrada == null) {
            throw rechazo("La fecha de entrada es obligatoria.");
        }
        String sql = "UPDATE productos SET nombre = ?, existencias = ?, lote = ?, caducidad = ?, fechaEntrada = ? "
                + "WHERE id = ? AND activo = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombreLimpio);
            ps.setInt(2, existencias);
            ps.setString(3, loteLimpio);
            ps.setString(4, caducidad.toString());
            ps.setString(5, fechaEntrada.toString());
            ps.setInt(6, id);
            if (ps.executeUpdate() == 0) {
                throw rechazo("El medicamento con ID " + id + " ya no existe.");
            }
        } catch (SQLException e) {
            throw errorDatos("actualizar el producto", e);
        }
    }

    /**
     * [FIX-009] Borrado lógico: el producto deja de aparecer en inventario, búsquedas,
     * alertas, apartados y reportes, pero su historial de ventas se conserva. También
     * se libera su apartado.
     *
     * @throws OperacionRechazadaException si el producto no existe o ya estaba eliminado.
     */
    public void eliminarProducto(int id) throws OperacionRechazadaException {
        String sql = "UPDATE productos SET activo = 0, fecha_separado = NULL WHERE id = ? AND activo = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw rechazo("El medicamento con ID " + id + " no existe o ya fue eliminado.");
            }
        } catch (SQLException e) {
            throw errorDatos("eliminar el producto", e);
        }
    }

    /**
     * [MR-002 / FIX-009] Busca un producto activo por ID exacto o por nombre completo.
     *
     * <ol>
     *   <li>Si {@code dato} es un entero, busca el producto con ese ID (el usuario
     *       eligió un lote concreto).</li>
     *   <li>Si no es número o no hay resultado, busca por nombre completo, sin distinguir
     *       mayúsculas ni acentos en mayúscula/minúscula. Con varios lotes del mismo nombre
     *       devuelve el <b>vendible</b> (con existencias y sin caducar) que caduca primero
     *       (FEFO); si ninguno es vendible, el primero que coincida, para que la operación
     *       posterior explique el motivo (caducado, sin existencias).</li>
     * </ol>
     *
     * @return el producto, o vacío si no hay coincidencias.
     */
    public Optional<Producto> buscarProductoPorIdONombre(String dato) {
        if (dato == null || dato.isBlank()) {
            return Optional.empty();
        }
        String texto = dato.trim();
        try {
            Optional<Producto> porId = buscarActivoPorId(Integer.parseInt(texto));
            if (porId.isPresent()) {
                return porId;
            }
        } catch (NumberFormatException e) {
            // No es un número: se busca por nombre
        }
        return buscarMejorLotePorNombre(texto);
    }

    private Optional<Producto> buscarActivoPorId(int id) {
        List<Producto> encontrados = consultarProductos("WHERE id = ? AND activo = 1", id);
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.get(0));
    }

    /**
     * La comparación se hace en Java porque {@code lower()} de SQLite solo maneja ASCII.
     */
    private Optional<Producto> buscarMejorLotePorNombre(String nombre) {
        LocalDate hoy = LocalDate.now();
        Producto mejor = null;
        YearMonth caducidadMejor = null;
        Producto primeraCoincidencia = null;
        for (Producto p : obtenerProductos()) {
            if (p.nombre() == null || !p.nombre().trim().equalsIgnoreCase(nombre)) {
                continue;
            }
            if (primeraCoincidencia == null) {
                primeraCoincidencia = p;
            }
            YearMonth caducidad = ReglasCaducidad.parsear(p.caducidad());
            boolean vendible = p.existencias() > 0
                    && caducidad != null
                    && !ReglasCaducidad.estaCaducado(caducidad, hoy);
            if (vendible && (mejor == null || caducidad.isBefore(caducidadMejor))) {
                mejor = p;
                caducidadMejor = caducidad;
            }
        }
        return Optional.ofNullable(mejor != null ? mejor : primeraCoincidencia);
    }

    // =========================================================================
    // Caducidad
    // =========================================================================

    /**
     * [MR-003] Productos activos que caducan dentro de los próximos {@code diasUmbral} días
     * (sin incluir los ya caducados). Los registros con caducidad inválida se omiten.
     */
    public List<Producto> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        LocalDate hoy = LocalDate.now();
        return obtenerProductos().stream()
                .filter(p -> ReglasCaducidad.estado(p.caducidad(), hoy, diasUmbral) == ReglasCaducidad.Estado.PROXIMO)
                .toList();
    }

    /**
     * [MR-003] Productos activos ya caducados. Los registros con caducidad inválida se omiten.
     */
    public List<Producto> obtenerMedicamentosCaducados() {
        LocalDate hoy = LocalDate.now();
        return obtenerProductos().stream()
                .filter(p -> ReglasCaducidad.estado(p.caducidad(), hoy, ReglasCaducidad.DIAS_ALERTA)
                        == ReglasCaducidad.Estado.CADUCADO)
                .toList();
    }

    // =========================================================================
    // Ventas
    // =========================================================================

    /** @return historial de ventas en orden de registro. */
    public List<Venta> obtenerHistorialVentas() {
        String sql = "SELECT id, idProducto, nombre, cantidad, fechaVenta FROM historial_ventas ORDER BY id ASC";
        List<Venta> ventas = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ventas.add(new Venta(rs.getInt("id"), rs.getInt("idProducto"), rs.getString("nombre"),
                        rs.getInt("cantidad"), rs.getString("fechaVenta")));
            }
            return ventas;
        } catch (SQLException e) {
            throw errorDatos("consultar el historial de ventas", e);
        }
    }

    /**
     * Registra una venta con fecha de hoy y descuenta las existencias (transacción).
     *
     * @return la venta registrada.
     * @throws OperacionRechazadaException si la cantidad no es positiva, el producto no
     *         existe, su caducidad es inválida o ya caducó, o no hay existencias suficientes.
     */
    public Venta registrarVenta(int idProducto, int cantidad) throws OperacionRechazadaException {
        if (cantidad <= 0) {
            throw rechazo("La cantidad vendida debe ser mayor a cero.");
        }
        Producto producto = buscarActivoPorId(idProducto)
                .orElseThrow(() -> rechazo("El producto con ID " + idProducto + " no existe."));
        if (cantidad > producto.existencias()) {
            throw rechazo("No hay suficientes existencias de " + producto.nombre()
                    + " (disponibles: " + producto.existencias() + ").");
        }
        validarVendible(producto, "la venta");

        String fecha = LocalDate.now().toString();
        try {
            int idVenta = enTransaccion(() -> {
                descontarExistencias(idProducto, cantidad);
                String sql = "INSERT INTO historial_ventas (idProducto, nombre, cantidad, fechaVenta) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idProducto);
                    ps.setString(2, producto.nombre());
                    ps.setInt(3, cantidad);
                    ps.setString(4, fecha);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        return keys.next() ? keys.getInt(1) : -1;
                    }
                }
            });
            return new Venta(idVenta, idProducto, producto.nombre(), cantidad, fecha);
        } catch (SQLException e) {
            throw errorDatos("registrar la venta", e);
        }
    }

    /**
     * Cambia la cantidad y fecha de una venta y ajusta el stock por la diferencia, en una
     * sola transacción (FIX-009).
     *
     * @throws OperacionRechazadaException si la cantidad no es positiva, falta la fecha, la
     *         venta o su producto no existen, o no hay existencias para aumentar la venta.
     */
    public void editarVenta(int idVenta, int nuevaCantidad, LocalDate nuevaFecha) throws OperacionRechazadaException {
        if (nuevaCantidad <= 0) {
            throw rechazo("La cantidad debe ser un número mayor a cero.");
        }
        if (nuevaFecha == null) {
            throw rechazo("La fecha de venta es obligatoria.");
        }
        Venta venta = buscarVenta(idVenta);
        int diferencia = nuevaCantidad - venta.cantidad();
        Integer stock = existenciasDe(venta.idProducto());
        if (stock == null) {
            throw rechazo("No se encontró el producto asociado a esta venta.");
        }
        if (diferencia > stock) {
            throw rechazo("No hay suficientes existencias para aumentar la venta (disponibles: " + stock + ").");
        }
        try {
            enTransaccion(() -> {
                descontarExistencias(venta.idProducto(), diferencia);
                String sql = "UPDATE historial_ventas SET cantidad = ?, fechaVenta = ? WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, nuevaCantidad);
                    ps.setString(2, nuevaFecha.toString());
                    ps.setInt(3, idVenta);
                    if (ps.executeUpdate() == 0) {
                        throw rechazo("La venta " + idVenta + " ya no existe.");
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            throw errorDatos("editar la venta", e);
        }
    }

    /**
     * [FIX-009] Elimina una venta y regresa la cantidad vendida al stock (cancelación), en
     * una sola transacción. Si el producto ya no existe físicamente (ventas huérfanas de
     * versiones anteriores), solo se elimina la venta.
     *
     * @throws OperacionRechazadaException si la venta no existe.
     */
    public void eliminarVenta(int idVenta) throws OperacionRechazadaException {
        Venta venta = buscarVenta(idVenta);
        try {
            enTransaccion(() -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE productos SET existencias = existencias + ? WHERE id = ?")) {
                    ps.setInt(1, venta.cantidad());
                    ps.setInt(2, venta.idProducto());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM historial_ventas WHERE id = ?")) {
                    ps.setInt(1, idVenta);
                    if (ps.executeUpdate() == 0) {
                        throw rechazo("La venta " + idVenta + " ya no existe.");
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            throw errorDatos("eliminar la venta", e);
        }
    }

    private Venta buscarVenta(int idVenta) throws OperacionRechazadaException {
        String sql = "SELECT id, idProducto, nombre, cantidad, fechaVenta FROM historial_ventas WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw rechazo("No se encontró la venta con ID " + idVenta + ".");
                }
                return new Venta(rs.getInt("id"), rs.getInt("idProducto"), rs.getString("nombre"),
                        rs.getInt("cantidad"), rs.getString("fechaVenta"));
            }
        } catch (SQLException e) {
            throw errorDatos("consultar la venta", e);
        }
    }

    /** @return existencias del producto (activo o no), o {@code null} si no existe. */
    private Integer existenciasDe(int idProducto) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT existencias FROM productos WHERE id = ?")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        } catch (SQLException e) {
            throw errorDatos("consultar existencias", e);
        }
    }

    /**
     * Resta {@code cantidad} (puede ser negativa para devolver) sin permitir stock negativo,
     * aunque las existencias hayan cambiado desde que se validaron.
     */
    private void descontarExistencias(int idProducto, int cantidad) throws SQLException, OperacionRechazadaException {
        String sql = "UPDATE productos SET existencias = existencias - ? WHERE id = ? AND existencias - ? >= 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, idProducto);
            ps.setInt(3, cantidad);
            if (ps.executeUpdate() == 0) {
                throw rechazo("No hay suficientes existencias.");
            }
        }
    }

    // =========================================================================
    // Apartados
    // =========================================================================

    /** @return productos activos con un apartado registrado. */
    public List<Producto> obtenerProductosApartados() {
        return consultarProductos("WHERE activo = 1 AND fecha_separado IS NOT NULL AND fecha_separado <> ''");
    }

    /**
     * [MR-004] Registra el apartado de un producto.
     *
     * @throws OperacionRechazadaException si falta la fecha, el producto no existe, su
     *         caducidad es inválida o ya caducó, o no tiene existencias.
     */
    public void separarProducto(int id, LocalDate fechaApartado) throws OperacionRechazadaException {
        if (fechaApartado == null) {
            throw rechazo("La fecha de apartado es obligatoria.");
        }
        Producto producto = buscarActivoPorId(id)
                .orElseThrow(() -> rechazo("El producto con ID " + id + " no existe."));
        validarVendible(producto, "el apartado");
        if (producto.existencias() <= 0) {
            throw rechazo("No se puede registrar el apartado: el medicamento no tiene existencias disponibles.");
        }
        actualizarFechaApartado(id, fechaApartado.toString());
    }

    /** @throws OperacionRechazadaException si falta la fecha o el producto no existe. */
    public void editarApartado(int id, LocalDate nuevaFecha) throws OperacionRechazadaException {
        if (nuevaFecha == null) {
            throw rechazo("La fecha de apartado es obligatoria.");
        }
        actualizarFechaApartado(id, nuevaFecha.toString());
    }

    /** @throws OperacionRechazadaException si el producto no existe. */
    public void eliminarApartado(int id) throws OperacionRechazadaException {
        actualizarFechaApartado(id, null);
    }

    private void actualizarFechaApartado(int id, String fecha) throws OperacionRechazadaException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE productos SET fecha_separado = ? WHERE id = ? AND activo = 1")) {
            ps.setString(1, fecha);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) {
                throw rechazo("El producto con ID " + id + " no existe.");
            }
        } catch (SQLException e) {
            throw errorDatos("actualizar el apartado", e);
        }
    }

    // =========================================================================
    // Configuración de la veterinaria
    // =========================================================================

    /**
     * [MR-005] Guarda la configuración de la clínica (siempre un único registro, {@code id = 1}).
     *
     * @throws OperacionRechazadaException si falta el nombre de la veterinaria.
     */
    public void guardarConfiguracion(ConfiguracionVeterinaria config) throws OperacionRechazadaException {
        Objects.requireNonNull(config, "config");
        requerido(config.nombre(), "El nombre de la veterinaria es obligatorio.");
        String sql = "INSERT OR REPLACE INTO configuracion_veterinaria (id, logo, nombre, direccion, telefono, rfc, horarios) "
                + "VALUES (1, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.logoBase64());
            ps.setString(2, config.nombre().trim());
            ps.setString(3, config.direccion());
            ps.setString(4, config.telefono());
            ps.setString(5, config.rfc());
            ps.setString(6, config.horarios());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw errorDatos("guardar la configuración", e);
        }
    }

    /** @return la configuración guardada, o vacío si nunca se ha configurado. */
    public Optional<ConfiguracionVeterinaria> obtenerConfiguracion() {
        String sql = "SELECT logo, nombre, direccion, telefono, rfc, horarios FROM configuracion_veterinaria LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new ConfiguracionVeterinaria(rs.getString("logo"), rs.getString("nombre"),
                    rs.getString("direccion"), rs.getString("telefono"), rs.getString("rfc"),
                    rs.getString("horarios")));
        } catch (SQLException e) {
            throw errorDatos("consultar la configuración", e);
        }
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private List<Producto> consultarProductos(String condicion, Object... parametros) {
        String sql = "SELECT " + COLUMNAS_PRODUCTO + " FROM productos " + condicion + " ORDER BY id";
        List<Producto> productos = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(new Producto(rs.getInt("id"), rs.getString("nombre"), rs.getInt("existencias"),
                            rs.getString("lote"), rs.getString("caducidad"), rs.getString("fechaEntrada"),
                            rs.getString("fecha_separado")));
                }
            }
            return productos;
        } catch (SQLException e) {
            throw errorDatos("consultar productos", e);
        }
    }

    /** Valida que el producto se pueda vender o apartar hoy (caducidad válida y vigente). */
    private static void validarVendible(Producto producto, String operacion) throws OperacionRechazadaException {
        YearMonth caducidad = ReglasCaducidad.parsear(producto.caducidad());
        if (caducidad == null) {
            throw rechazo("No se puede registrar " + operacion + ": la caducidad del producto no tiene un formato válido.");
        }
        if (ReglasCaducidad.estaCaducado(caducidad, LocalDate.now())) {
            throw rechazo("No se puede registrar " + operacion + ": el medicamento está caducado.");
        }
    }

    private static void validarExistencias(int existencias) throws OperacionRechazadaException {
        if (existencias < 0) {
            throw rechazo("Las existencias no pueden ser negativas.");
        }
    }

    private static String requerido(String valor, String mensaje) throws OperacionRechazadaException {
        if (valor == null || valor.isBlank()) {
            throw rechazo(mensaje);
        }
        return valor.trim();
    }

    private static OperacionRechazadaException rechazo(String mensaje) {
        return new OperacionRechazadaException(mensaje);
    }

    private static ErrorDatosException errorDatos(String accion, SQLException causa) {
        return new ErrorDatosException("Error de base de datos al " + accion + ": " + causa.getMessage(), causa);
    }

    /** Operación de BD que puede fallar o ser rechazada. */
    @FunctionalInterface
    private interface OperacionSql<T> {
        T ejecutar() throws SQLException, OperacionRechazadaException;
    }

    /**
     * Ejecuta {@code operacion} en una transacción: confirma si termina bien y deshace
     * todos los cambios ante cualquier excepción (incluido un rechazo de negocio).
     */
    private <T> T enTransaccion(OperacionSql<T> operacion) throws SQLException, OperacionRechazadaException {
        boolean autoCommitPrevio = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T resultado = operacion.ejecutar();
            connection.commit();
            return resultado;
        } catch (SQLException | OperacionRechazadaException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommitPrevio);
        }
    }
}
