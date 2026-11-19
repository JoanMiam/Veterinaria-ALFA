package view;

import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;
import model.Producto;
import model.ReglasCaducidad;

/**
 * Formulario para editar un medicamento (lote) existente.
 */
public class EditarMedicamentoView extends FormularioConfirmableDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final Producto producto;
    private final JTextField txtNombre;
    private final JSpinner spinnerExistencias;
    private final JTextField txtLote;
    private final Fechas.SelectorCaducidad selectorCaducidad;
    private final JDateChooser selectorEntrada;

    public EditarMedicamentoView(JFrame parent, InventarioController controller, Producto producto) {
        super(parent, "Editar Medicamento", true);
        this.controller = controller;
        this.producto = producto;
        txtNombre = Estilos.campoTexto(producto.nombre());
        spinnerExistencias = Estilos.spinnerEntero(producto.existencias(), 0);
        txtLote = Estilos.campoTexto(producto.lote());
        // [MR-010] El año de una caducidad pasada se conserva (antes se cambiaba al año actual).
        selectorCaducidad = new Fechas.SelectorCaducidad(ReglasCaducidad.parsear(producto.caducidad()));
        selectorEntrada = Fechas.selectorDia(Fechas.parsear(producto.fechaEntrada()));

        Formularios.Rejilla rejilla = new Formularios.Rejilla()
                .fila("Nombre:", txtNombre)
                .fila("Existencias:", spinnerExistencias)
                .fila("Lote:", txtLote)
                .fila("Caducidad (YYYY-MM):", selectorCaducidad)
                .fila("Fecha de Entrada (YYYY-MM-DD):", selectorEntrada);
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
        boolean ok = Dialogos.ejecutar(this, () -> controller.editarProducto(
                producto.id(),
                txtNombre.getText(),
                (Integer) spinnerExistencias.getValue(),
                txtLote.getText(),
                selectorCaducidad.getValor(),
                Fechas.valor(selectorEntrada)));
        if (ok) {
            Dialogos.info(getOwner(), "Medicamento actualizado con éxito.");
            dispose();
        }
    }
}
