package controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import model.ConfiguracionVeterinaria;
import model.InventarioDAO;
import model.OperacionRechazadaException;
import model.Producto;
import model.ReportesHtml;
import model.Venta;

/**
 * [MR-010] Capa de servicio entre las vistas y el DAO.
 *
 * <ul>
 *   <li>Expone operaciones con tipos de dominio ({@link Producto}, {@link Venta},
 *       {@link YearMonth}, {@link LocalDate}) en lugar de cadenas y {@code Object[]}.</li>
 *   <li>Las reglas de negocio no cumplidas llegan como {@link OperacionRechazadaException};
 *       la vista muestra el mensaje.</li>
 *   <li>Tras cada cambio que afecta al inventario avisa a los suscriptores
 *       ({@link #alCambiarInventario(Runnable)}), en lugar de llamar directamente a la vista.</li>
 *   <li>Genera los reportes HTML ({@link ReportesHtml}) y los escribe en disco.</li>
 * </ul>
 */
public class InventarioController {

    private final InventarioDAO dao;
    private final List<Runnable> suscriptores = new CopyOnWriteArrayList<>();

    public InventarioController(InventarioDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    /** Registra una acción a ejecutar cada vez que cambia el inventario (stock, productos, apartados). */
    public void alCambiarInventario(Runnable accion) {
        suscriptores.add(Objects.requireNonNull(accion, "accion"));
    }

    private void notificarCambio() {
        suscriptores.forEach(Runnable::run);
    }

    // =========================================================================
    // Productos
    // =========================================================================

    public List<Producto> obtenerProductos() {
        return dao.obtenerProductos();
    }

    public Producto agregarProducto(String nombre, int existencias, String lote, YearMonth caducidad)
            throws OperacionRechazadaException {
        Producto creado = dao.agregarProducto(nombre, existencias, lote, caducidad);
        notificarCambio();
        return creado;
    }

    public void editarProducto(int id, String nombre, int existencias, String lote,
                               YearMonth caducidad, LocalDate fechaEntrada) throws OperacionRechazadaException {
        dao.editarProducto(id, nombre, existencias, lote, caducidad, fechaEntrada);
        notificarCambio();
    }

    /**
     * Elimina (oculta) varios productos. Continúa aunque alguno falle.
     *
     * @return mensajes de los productos que no se pudieron eliminar (vacío si todos se eliminaron).
     */
    public List<String> eliminarProductos(List<Integer> ids) {
        List<String> errores = new ArrayList<>();
        for (int id : ids) {
            try {
                dao.eliminarProducto(id);
            } catch (OperacionRechazadaException e) {
                errores.add(e.getMessage());
            }
        }
        notificarCambio();
        return errores;
    }

    /** [MR-002] Busca por ID exacto o nombre completo (FEFO entre lotes del mismo nombre). */
    public Optional<Producto> buscarProducto(String idONombre) {
        return dao.buscarProductoPorIdONombre(idONombre);
    }

    public List<Producto> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        return dao.obtenerMedicamentosProximosACaducar(diasUmbral);
    }

    public List<Producto> obtenerMedicamentosCaducados() {
        return dao.obtenerMedicamentosCaducados();
    }

    // =========================================================================
    // Ventas
    // =========================================================================

    public List<Venta> obtenerHistorialVentas() {
        return dao.obtenerHistorialVentas();
    }

    /**
     * [MR-002] Busca el medicamento por ID o nombre y registra la venta.
     *
     * @throws OperacionRechazadaException si no se encuentra el medicamento o la venta no procede.
     */
    public Venta registrarVenta(String idONombre, int cantidad) throws OperacionRechazadaException {
        Producto producto = buscarProducto(idONombre)
                .orElseThrow(() -> new OperacionRechazadaException(
                        "No se encontró ningún medicamento con el ID o nombre ingresado."));
        Venta venta = dao.registrarVenta(producto.id(), cantidad);
        notificarCambio();
        return venta;
    }

    public void editarVenta(int idVenta, int nuevaCantidad, LocalDate nuevaFecha) throws OperacionRechazadaException {
        dao.editarVenta(idVenta, nuevaCantidad, nuevaFecha);
        notificarCambio();
    }

    /**
     * [FIX-009] Elimina ventas regresando su cantidad al stock. Continúa aunque alguna falle.
     *
     * @return mensajes de las ventas que no se pudieron eliminar.
     */
    public List<String> eliminarVentas(List<Integer> ids) {
        List<String> errores = new ArrayList<>();
        for (int id : ids) {
            try {
                dao.eliminarVenta(id);
            } catch (OperacionRechazadaException e) {
                errores.add(e.getMessage());
            }
        }
        notificarCambio();
        return errores;
    }

    // =========================================================================
    // Apartados
    // =========================================================================

    public List<Producto> obtenerProductosApartados() {
        return dao.obtenerProductosApartados();
    }

    /**
     * [MR-004] Busca el medicamento por ID o nombre y registra el apartado.
     *
     * @throws OperacionRechazadaException si no se encuentra el medicamento o el apartado no procede.
     */
    public void apartarProducto(String idONombre, LocalDate fecha) throws OperacionRechazadaException {
        Producto producto = buscarProducto(idONombre)
                .orElseThrow(() -> new OperacionRechazadaException(
                        "No se encontró el producto con el dato proporcionado."));
        dao.separarProducto(producto.id(), fecha);
        notificarCambio();
    }

    public void editarApartado(int idProducto, LocalDate nuevaFecha) throws OperacionRechazadaException {
        dao.editarApartado(idProducto, nuevaFecha);
        notificarCambio();
    }

    /** @return mensajes de los apartados que no se pudieron eliminar. */
    public List<String> eliminarApartados(List<Integer> idsProducto) {
        List<String> errores = new ArrayList<>();
        for (int id : idsProducto) {
            try {
                dao.eliminarApartado(id);
            } catch (OperacionRechazadaException e) {
                errores.add(e.getMessage());
            }
        }
        notificarCambio();
        return errores;
    }

    // =========================================================================
    // Configuración y reportes
    // =========================================================================

    public Optional<ConfiguracionVeterinaria> obtenerConfiguracion() {
        return dao.obtenerConfiguracion();
    }

    public void guardarConfiguracion(ConfiguracionVeterinaria config) throws OperacionRechazadaException {
        dao.guardarConfiguracion(config);
    }

    /** [MR-005] Escribe el reporte HTML del inventario (productos activos). */
    public void exportarInventario(File destino) throws IOException {
        escribir(destino, ReportesHtml.inventario(dao.obtenerProductos(), configuracionOnull()));
    }

    /** [MR-005] Escribe el reporte HTML del historial de ventas. */
    public void exportarVentas(File destino) throws IOException {
        escribir(destino, ReportesHtml.ventas(dao.obtenerHistorialVentas(), configuracionOnull()));
    }

    /** [MR-005] Escribe el reporte HTML de los productos apartados. */
    public void exportarApartados(File destino) throws IOException {
        escribir(destino, ReportesHtml.apartados(dao.obtenerProductosApartados(), configuracionOnull()));
    }

    private ConfiguracionVeterinaria configuracionOnull() {
        return dao.obtenerConfiguracion().orElse(null);
    }

    private static void escribir(File destino, String html) throws IOException {
        Files.writeString(destino.toPath(), html, StandardCharsets.UTF_8);
    }
}
