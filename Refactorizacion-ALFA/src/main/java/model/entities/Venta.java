package model.entities;

public class Venta {
    private int id;
    private int idProducto;
    private String nombre;
    private int cantidad;
    private String fechaVenta;

    public Venta() {
    }

    public Venta(int id, int idProducto, String nombre, int cantidad, String fechaVenta) {
        this.id = id;
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.fechaVenta = fechaVenta;
    }

    public Venta(int idProducto, String nombre, int cantidad, String fechaVenta) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.fechaVenta = fechaVenta;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(String fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    @Override
    public String toString() {
        return "Venta{" +
                "id=" + id +
                ", idProducto=" + idProducto +
                ", nombre='" + nombre + '\'' +
                ", cantidad=" + cantidad +
                ", fechaVenta='" + fechaVenta + '\'' +
                '}';
    }
}
