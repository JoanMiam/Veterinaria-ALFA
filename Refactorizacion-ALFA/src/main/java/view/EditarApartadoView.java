package view;

import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;
import model.Producto;

/**
 * Edición de la fecha de un apartado.
 */
public class EditarApartadoView extends FormularioConfirmableDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final ApartadosView parentView;
    private final Producto producto;
    private final JDateChooser selectorFecha;

    public EditarApartadoView(ApartadosView parentView, InventarioController controller, Producto producto) {
        super(parentView, "Editar Apartado", true);
        this.controller = controller;
        this.parentView = parentView;
        this.producto = producto;

        JTextField txtProducto = Estilos.campoTexto(producto.nombre() + " (lote " + producto.lote() + ")");
        txtProducto.setEditable(false);
        selectorFecha = Fechas.selectorDia(Fechas.parsear(producto.fechaApartado()));

        Formularios.Rejilla rejilla = new Formularios.Rejilla()
                .fila("Producto:", txtProducto)
                .fila("Fecha de Apartado (YYYY-MM-DD):", selectorFecha);
        Formularios.mostrar(this, rejilla, this::guardar, this::confirmarCierre);
    }

    /**
     * [MR-007 – OPC-B] El formulario se abre con la fecha precargada: cerrar sin guardar
     * podría descartar cambios.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return true;
    }

    private void guardar() {
        boolean ok = Dialogos.ejecutar(this, () ->
                controller.editarApartado(producto.id(), Fechas.valor(selectorFecha)));
        if (ok) {
            Dialogos.info(this, "Apartado actualizado exitosamente.");
            parentView.actualizarTabla();
            dispose();
        }
    }
}
