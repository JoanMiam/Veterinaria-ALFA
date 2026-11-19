package view;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import controller.InventarioController;

/**
 * Formulario para registrar un medicamento (lote) nuevo.
 */
public class AgregarMedicamentoView extends FormularioConfirmableDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final JTextField txtNombre = Estilos.campoTexto("");
    private final JSpinner spinnerExistencias = Estilos.spinnerEntero(0, 0);
    private final JTextField txtLote = Estilos.campoTexto("");
    private final Fechas.SelectorCaducidad selectorCaducidad = new Fechas.SelectorCaducidad(null);

    public AgregarMedicamentoView(JFrame parent, InventarioController controller) {
        super(parent, "Agregar Medicamento", true);
        this.controller = controller;
        Formularios.Rejilla rejilla = new Formularios.Rejilla()
                .fila("Nombre:", txtNombre)
                .fila("Existencias:", spinnerExistencias)
                .fila("Lote:", txtLote)
                .fila("Caducidad (YYYY-MM):", selectorCaducidad);
        Formularios.mostrar(this, rejilla, this::guardar, this::confirmarCierre);
    }

    /**
     * [MR-007 – OPC-B] Hay datos que perder si se escribió nombre o lote.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return !txtNombre.getText().trim().isEmpty() || !txtLote.getText().trim().isEmpty();
    }

    private void guardar() {
        boolean ok = Dialogos.ejecutar(this, () -> controller.agregarProducto(
                txtNombre.getText(),
                (Integer) spinnerExistencias.getValue(),
                txtLote.getText(),
                selectorCaducidad.getValor()));
        if (ok) {
            Dialogos.info(getOwner(), "Medicamento agregado con éxito.");
            dispose();
        }
    }
}
