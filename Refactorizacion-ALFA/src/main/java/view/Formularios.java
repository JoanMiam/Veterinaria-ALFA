package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * [MR-010] Construcción común de formularios "etiqueta: campo" con botones Guardar/Cancelar.
 */
final class Formularios {

    private Formularios() { }

    /** Panel con filas {@code etiqueta | campo}. */
    static final class Rejilla extends JPanel {
        private static final long serialVersionUID = 1L;
        private final GridBagConstraints gbc = new GridBagConstraints();
        private int fila = 0;

        Rejilla() {
            super(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
            gbc.insets = new Insets(8, 10, 8, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
        }

        Rejilla fila(String etiqueta, JComponent campo) {
            gbc.gridy = fila++;
            gbc.gridx = 0;
            gbc.weightx = 0;
            add(Estilos.etiqueta(etiqueta), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            add(campo, gbc);
            return this;
        }
    }

    /**
     * Arma el diálogo: rejilla al centro y botones Guardar/Cancelar abajo, lo centra
     * sobre su padre y lo muestra (modal).
     */
    static void mostrar(JDialog dialogo, Rejilla rejilla, Runnable alGuardar, Runnable alCancelar) {
        JButton btnGuardar = Estilos.botonPrimario("Guardar");
        JButton btnCancelar = Estilos.botonPeligro("Cancelar");
        btnGuardar.addActionListener(e -> alGuardar.run());
        btnCancelar.addActionListener(e -> alCancelar.run());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        botones.add(btnGuardar);
        botones.add(btnCancelar);

        dialogo.getContentPane().setLayout(new BorderLayout());
        dialogo.getContentPane().add(rejilla, BorderLayout.CENTER);
        dialogo.getContentPane().add(botones, BorderLayout.SOUTH);
        dialogo.getRootPane().setDefaultButton(btnGuardar);
        dialogo.setResizable(false);
        dialogo.pack();
        dialogo.setLocationRelativeTo(dialogo.getOwner());
        dialogo.setVisible(true);
    }
}
