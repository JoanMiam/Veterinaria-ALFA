package model.entities;


public class Apartado extends Producto {

    
    public Apartado() {
        super();
    }

    
    public Apartado(int id, String nombre, int existencias, String lote,
            String caducidad, String fechaEntrada, String fechaSeparado) {
        super(id, nombre, existencias, lote, caducidad, fechaEntrada, fechaSeparado);
    }

    
    public boolean tieneApartadoValido() {
        return getFechaSeparado() != null && !getFechaSeparado().isEmpty();
    }

    @Override
    public String toString() {
        return "Apartado{" +
                "id=" + getId() +
                ", nombre='" + getNombre() + '\'' +
                ", existencias=" + getExistencias() +
                ", lote='" + getLote() + '\'' +
                ", caducidad='" + getCaducidad() + '\'' +
                ", fechaEntrada='" + getFechaEntrada() + '\'' +
                ", fechaSeparado='" + getFechaSeparado() + '\'' +
                '}';
    }
}
