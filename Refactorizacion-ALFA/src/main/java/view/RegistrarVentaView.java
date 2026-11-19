package view;

import javax.swing.JSpinner;
import javax.swing.JTextField;

import controller.InventarioController;
import model.Venta;

/**
 * [MR-002] Registro de una venta: un solo campo acepta el ID o el nombre del
 * medicamento. Con varios lotes del mismo nombre se usa el que caduca primero (FEFO).
 */
public class RegistrarVentaView extends FormularioConfirmableDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final VentasView parentView;
    private final JTextField txtBusqueda = Estilos.campoTexto("");
    private final JSpinner spinnerCantidad = Estilos.spinnerEntero(1, 1);

    public RegistrarVentaView(VentasView parentView, InventarioController controller) {
        super(parentView, "Agregar Venta", true);
        this.controller = controller;
        this.parentView = parentView;
        Formularios.Rejilla rejilla = new Formularios.Rejilla()
                .fila("ID o Nombre del Medicamento:", txtBusqueda)
                .fila("Cantidad Vendida:", spinnerCantidad);
        Formularios.mostrar(this, rejilla, this::registrar, this::confirmarCierre);
    }

    /** [MR-007 – OPC-B] Hay datos que perder si se escribió el medicamento. */
    @Override
    protected boolean tieneDatosIngresados() {
        return !txtBusqueda.getText().trim().isEmpty();
    }

    private void registrar() {
        if (txtBusqueda.getText().isBlank()) {
            Dialogos.error(this, "Ingrese el ID o el nombre del medicamento.");
            return;
        }
        Venta[] registrada = new Venta[1];
        boolean ok = Dialogos.ejecutar(this, () ->
                registrada[0] = controller.registrarVenta(txtBusqueda.getText(), (Integer) spinnerCantidad.getValue()));
        if (ok) {
            Dialogos.info(this, "Venta registrada con éxito: " + registrada[0].cantidad() + " × "
                    + registrada[0].nombre() + " (producto ID " + registrada[0].idProducto() + ").");
            parentView.actualizarTabla();
            dispose();
        }
    }
}
