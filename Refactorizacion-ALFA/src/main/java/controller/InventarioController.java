package controller;

import java.io.File;
import java.util.List;
import model.InventarioDAO;
import model.entities.Producto;
import model.entities.Venta;

public class InventarioController {
    private InventarioDAO dao;
    private view.InventarioView inventarioView;

    public InventarioController(InventarioDAO dao) {
        this.dao = dao;
    }

    public void setInventarioView(view.InventarioView inventarioView) {
        this.inventarioView = inventarioView;
    }

    
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
            inventarioView.cargarDatos(); 
        }
        return result;
    }

    public boolean eliminarVenta(Object id) {
        return dao.eliminarVenta(id);
    }

    public List<Producto> obtenerProductos() {
        return dao.obtenerProductos();
    }

    public List<Venta> obtenerHistorialVentas() {
        return dao.obtenerHistorialVentas();
    }

    public void exportarCSV(File fileToSave) {
        dao.exportarCSV(fileToSave);
    }

    public void exportarInventarioCSV(File fileToSave) {
        dao.exportarInventarioCSV(fileToSave);
    }

    
    public List<Producto> obtenerMedicamentosProximosACaducar(int diasUmbral) {
        return dao.obtenerMedicamentosProximosACaducar(diasUmbral);
    }

    public List<Producto> obtenerMedicamentosCaducados() {
        return dao.obtenerMedicamentosCaducados();
    }

    public boolean separarProducto(int id, String fechaSeparado) {
        boolean result = dao.separarProducto(id, fechaSeparado);
        if (result && inventarioView != null) {
            inventarioView.cargarDatos();
        }
        return result;
    }

    public List<Producto> obtenerProductosApartados() {
        return dao.obtenerProductosApartados();
    }

    public void exportarApartadosCSV(File fileToSave) {
        dao.exportarApartadosCSV(fileToSave);
    }

    public int obtenerProductoIdPorNombre(String dato) {
        return dao.obtenerProductoIdPorNombre(dato);
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
