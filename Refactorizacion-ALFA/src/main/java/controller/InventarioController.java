package controller;

import java.io.File;
import java.util.List;

import model.InventarioDAO;

public class InventarioController {
    private InventarioDAO dao;
    private view.InventarioView inventarioView;

    public InventarioController(InventarioDAO dao) {
        this.dao = dao;
    }

    public void setInventarioView(view.InventarioView inventarioView) {
        this.inventarioView = inventarioView;
    }

    // Gestión de productos
    public boolean agregarProducto(String nombre, String existencias, String lote, String caducidad) {
        boolean result = dao.agregarProducto(nombre, existencias, lote, caducidad);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }

    public boolean eliminarProducto(int id) {
        boolean result = dao.eliminarProducto(id);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }

    public boolean editarProducto(int id,
                                  String nombre,
                                  String existencias,
                                  String lote,
                                  String caducidad,
                                  String fechaEntrada) {
        boolean result = dao.editarProducto(id, nombre, existencias, lote, caducidad, fechaEntrada);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }


    public boolean registrarVenta(int id, String nombre, int cantidad) {
        boolean result = dao.registrarVenta(id, nombre, cantidad);
        if (result && inventarioView != null) {
            inventarioView.actualizarTabla();
        }
        return result;
    }





    public boolean editarVenta(Object id, String nuevoNombre, String nuevaCantidad, String nuevaFecha) {
        boolean result = dao.editarVenta(id, nuevoNombre, nuevaCantidad, nuevaFecha);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();  // Actualiza la vista de inventario para reflejar el stock actualizado.
        }
        return result;
    }


    public boolean eliminarVenta(Object id) {
        return dao.eliminarVenta(id);
    }

    public Object[][] obtenerProductos() {
        return dao.obtenerProductos();
    }

    public List<Object[]> obtenerHistorialVentas() {
        return dao.obtenerHistorialVentas();
    }

    /** [MR-005 – OPC-A] Delega la generación del reporte HTML de ventas al DAO. */
    public void exportarCSV(File fileToSave) {
        dao.exportarVentasHTML(fileToSave);
    }

    /** [MR-005 – OPC-A] Delega la generación del reporte HTML del inventario al DAO. */
    public void exportarInventarioCSV(File fileToSave) {
        dao.exportarInventarioHTML(fileToSave);
    }


    // Método agregado para obtener medicamentos próximos a caducar
    public List<Object[]> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        return dao.obtenerMedicamentosProximosACaducar(diasUmbral);
    }

    public List<Object[]> obtenerMedicamentosCaducados() {
        return dao.obtenerMedicamentosCaducados();
    }


    public boolean separarProducto(int id, String fechaSeparado) {
        boolean result = dao.separarProducto(id, fechaSeparado);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }

    public List<Object[]> obtenerProductosApartados() {
        return dao.obtenerProductosApartados();
    }

    /** [MR-005 – OPC-A] Delega la generación del reporte HTML de apartados al DAO. */
    public void exportarApartadosCSV(File fileToSave) {
        dao.exportarApartadosHTML(fileToSave);
    }

    /**
     * [MR-005 – OPC-A] Persiste la configuración de la veterinaria delegando al DAO.
     *
     * @param logo      Logotipo en Base64 (puede ser {@code null}).
     * @param nombre    Nombre de la clínica.
     * @param direccion Dirección física.
     * @param telefono  Teléfono de contacto.
     * @param rfc       RFC de la clínica.
     * @param horarios  Horarios de atención.
     */
    public void guardarConfiguracion(String logo, String nombre, String direccion,
                                     String telefono, String rfc, String horarios) {
        dao.guardarConfiguracion(logo, nombre, direccion, telefono, rfc, horarios);
    }

    /**
     * [MR-005 – OPC-A] Recupera la configuración de la veterinaria desde el DAO.
     *
     * @return Arreglo {@code {logo, nombre, direccion, telefono, rfc, horarios}} o
     *         {@code null} si no existe configuración guardada.
     */
    public Object[] obtenerConfiguracion() {
        return dao.obtenerConfiguracion();
    }

    public int obtenerProductoIdPorNombre(String dato) {
        return dao.obtenerProductoIdPorNombre(dato);
    }

    /**
     * [MR-002 – OPC-A] Wrapper que delega la búsqueda unificada al DAO.
     *
     * <p>Permite a la vista obtener el {@code id} y {@code nombre} reales de un
     * producto ingresando únicamente un ID numérico o el nombre exacto del
     * medicamento, sin necesidad de conocer la lógica de acceso a datos.
     *
     * @param dato Cadena ingresada por el usuario (ID numérico o nombre exacto).
     * @return {@code Object[]{id, nombre}} si el producto existe,
     *         {@code null} si no se encontró ninguna coincidencia.
     * @see model.InventarioDAO#buscarProductoPorIdONombre(String)
     */
    public Object[] buscarProductoPorIdONombre(String dato) {
        return dao.buscarProductoPorIdONombre(dato);
    }


    public boolean eliminarApartado(int id) {
        boolean result = dao.eliminarApartado(id);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }

    public boolean editarApartado(int id, String nuevaFechaApartado) {
        boolean result = dao.editarApartado(id, nuevaFechaApartado);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }



}
