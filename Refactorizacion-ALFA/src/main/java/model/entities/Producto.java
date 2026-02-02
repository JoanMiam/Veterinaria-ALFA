package model.entities;

public class Producto {
    private int id;
    private String nombre;
    private int existencias;
    private String lote;
    private String caducidad;
    private String fechaEntrada;
    private String fechaSeparado; 

    public Producto() {
    }

    public Producto(int id, String nombre, int existencias, String lote,
            String caducidad, String fechaEntrada, String fechaSeparado) {
        this.id = id;
        this.nombre = nombre;
        this.existencias = existencias;
        this.lote = lote;
        this.caducidad = caducidad;
        this.fechaEntrada = fechaEntrada;
        this.fechaSeparado = fechaSeparado;
    }

    public Producto(String nombre, int existencias, String lote,
            String caducidad, String fechaEntrada) {
        this.nombre = nombre;
        this.existencias = existencias;
        this.lote = lote;
        this.caducidad = caducidad;
        this.fechaEntrada = fechaEntrada;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getExistencias() {
        return existencias;
    }

    public void setExistencias(int existencias) {
        this.existencias = existencias;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public String getCaducidad() {
        return caducidad;
    }

    public void setCaducidad(String caducidad) {
        this.caducidad = caducidad;
    }

    public String getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(String fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public String getFechaSeparado() {
        return fechaSeparado;
    }

    public void setFechaSeparado(String fechaSeparado) {
        this.fechaSeparado = fechaSeparado;
    }

    public boolean estaApartado() {
        return fechaSeparado != null && !fechaSeparado.isEmpty();
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", existencias=" + existencias +
                ", lote='" + lote + '\'' +
                ", caducidad='" + caducidad + '\'' +
                ", fechaEntrada='" + fechaEntrada + '\'' +
                ", fechaSeparado='" + fechaSeparado + '\'' +
                '}';
    }
}
