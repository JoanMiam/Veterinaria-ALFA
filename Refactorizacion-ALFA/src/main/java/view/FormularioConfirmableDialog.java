package view;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * [MR-007 – OPC-B] Clase base abstracta para diálogos modales que requieren
 * confirmación antes de cerrarse cuando el usuario tiene datos sin guardar.
 *
 * <p>Centraliza la lógica de cierre seguro que antes se duplicaba en cada
 * formulario: establece {@code DO_NOTHING_ON_CLOSE}, registra un
 * {@link WindowListener} que intercepta el evento de cierre y delega la
 * decisión al método abstracto {@link #tieneDatosIngresados()}.
 *
 * <p>Las subclases deben:
 * <ol>
 *   <li>Llamar al constructor apropiado ({@link Frame} o {@link Dialog} como owner).</li>
 *   <li>Implementar {@link #tieneDatosIngresados()} según sus propios campos.</li>
 *   <li>Invocar {@link #confirmarCierre()} en el {@code ActionListener} del
 *       botón Cancelar en lugar de {@code dispose()} directamente.</li>
 * </ol>
 */
public abstract class FormularioConfirmableDialog extends JDialog {

    /**
     * Construye el diálogo con owner de tipo {@link Frame}.
     *
     * @param parent ventana padre (normalmente un {@link JFrame}).
     * @param title  título de la barra del diálogo.
     * @param modal  {@code true} para bloquear la ventana padre mientras el
     *               diálogo esté abierto.
     */
    protected FormularioConfirmableDialog(Frame parent, String title, boolean modal) {
        super(parent, title, modal);
        configurar();
    }

    /**
     * Construye el diálogo con owner de tipo {@link Dialog}.
     *
     * @param parent ventana padre (p. ej. otro {@link JDialog}).
     * @param title  título de la barra del diálogo.
     * @param modal  {@code true} para bloquear la ventana padre mientras el
     *               diálogo esté abierto.
     */
    protected FormularioConfirmableDialog(Dialog parent, String title, boolean modal) {
        super(parent, title, modal);
        configurar();
    }

    private void configurar() {
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmarCierre();
            }
        });
    }

    /**
     * Solicita confirmación al usuario antes de cerrar el diálogo.
     *
     * <p>Si {@link #tieneDatosIngresados()} retorna {@code false}, el diálogo
     * se cierra directamente. En caso contrario, muestra un
     * {@link JOptionPane} de confirmación; el diálogo solo se cierra si el
     * usuario elige "Sí".
     *
     * <p>Debe ser invocado por el {@code ActionListener} del botón Cancelar
     * de cada subclase en lugar de llamar a {@code dispose()} directamente.
     */
    protected void confirmarCierre() {
        if (!tieneDatosIngresados()) {
            dispose();
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de cerrar el formulario? Se perderán los datos ingresados.",
                "Confirmar cierre",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (opcion == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    /**
     * Indica si el formulario contiene datos ingresados por el usuario que
     * se perderían al cerrar sin guardar.
     *
     * <p>Cada subclase implementa este método inspeccionando sus propios campos.
     * Retornar {@code true} provoca que {@link #confirmarCierre()} muestre el
     * diálogo de advertencia; retornar {@code false} cierra el diálogo directamente.
     *
     * @return {@code true} si hay datos que podrían perderse al cerrar;
     *         {@code false} si el formulario está vacío o sin cambios.
     */
    protected abstract boolean tieneDatosIngresados();
}
