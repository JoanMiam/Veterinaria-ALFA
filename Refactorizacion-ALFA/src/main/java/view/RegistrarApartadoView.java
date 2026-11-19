package view;

import java.time.LocalDate;

import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;

/**
 * [MR-004] Registro de un apartado: el campo acepta el ID o el nombre del medicamento.
 */
public class RegistrarApartadoView extends FormularioConfirmableDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final ApartadosView parentView;
    private final JTextField txtBusqueda = Estilos.campoTexto("");
    private final JDateChooser selectorFecha = Fechas.selectorDia(LocalDate.now());

    public RegistrarApartadoView(ApartadosView parentView, InventarioController controller) {
        super(parentView, "Registrar Apartado", true);
        this.controller = controller;
        this.parentView = parentView;
        Formularios.Rejilla rejilla = new Formularios.Rejilla()
                .fila("Nombre o ID del Producto:", txtBusqueda)
                .fila("Fecha de Apartado (YYYY-MM-DD):", selectorFecha);
        Formularios.mostrar(this, rejilla, this::registrar, this::confirmarCierre);
    }

    /**
     * [MR-007 – OPC-B] Hay datos que perder si se escribió el producto. La fecha no se
     * evalúa porque ya viene con la de hoy.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return !txtBusqueda.getText().trim().isEmpty();
    }

    private void registrar() {
        if (txtBusqueda.getText().isBlank()) {
            Dialogos.error(this, "Ingrese el ID o el nombre del producto.");
            return;
        }
        boolean ok = Dialogos.ejecutar(this, () ->
                controller.apartarProducto(txtBusqueda.getText(), Fechas.valor(selectorFecha)));
        if (ok) {
            Dialogos.info(this, "Producto apartado exitosamente.");
            parentView.actualizarTabla();
            dispose();
        }
    }
}
