package view;

import java.time.LocalDate;

import javax.swing.JSpinner;
import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;
import model.Venta;

/**
 * Edición de una venta: cantidad y fecha. El stock se ajusta por la diferencia.
 *
 * <p>[MR-010] El nombre del medicamento se muestra solo como referencia: antes era
 * editable pero tenía que coincidir exactamente con el del producto, así que no servía.
 */
public class EditarVentaView extends FormularioConfirmableDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final VentasView parentView;
    private final Venta venta;
    private final JSpinner spinnerCantidad;
    private final JDateChooser selectorFecha;

    public EditarVentaView(VentasView parentView, InventarioController controller, Venta venta) {
        super(parentView, "Editar Venta", true);
        this.controller = controller;
        this.parentView = parentView;
        this.venta = venta;

        JTextField txtNombre = Estilos.campoTexto(venta.nombre());
        txtNombre.setEditable(false);
        spinnerCantidad = Estilos.spinnerEntero(venta.cantidad(), 1);
        LocalDate fecha = Fechas.parsear(venta.fecha());
        selectorFecha = Fechas.selectorDia(fecha != null ? fecha : LocalDate.now());

        Formularios.Rejilla rejilla = new Formularios.Rejilla()
                .fila("Medicamento:", txtNombre)
                .fila("Cantidad Vendida:", spinnerCantidad)
                .fila("Fecha de Venta (YYYY-MM-DD):", selectorFecha);
        Formularios.mostrar(this, rejilla, this::guardar, this::confirmarCierre);
    }

    /**
     * [MR-007 – OPC-B] El formulario se abre con datos precargados: cerrar sin guardar
     * podría descartar cambios.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return true;
    }

    private void guardar() {
        boolean ok = Dialogos.ejecutar(this, () -> controller.editarVenta(
                venta.id(), (Integer) spinnerCantidad.getValue(), Fechas.valor(selectorFecha)));
        if (ok) {
            Dialogos.info(parentView, "Venta actualizada con éxito.");
            parentView.actualizarTabla();
            dispose();
        }
    }
}
