package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;

public class EditarApartadoView extends FormularioConfirmableDialog {
    private JDateChooser dateChooserApartado;
    private InventarioController controller;
    private ApartadosView parentView;
    private int productoId;

    public EditarApartadoView(ApartadosView parentView, InventarioController controller, int productoId, String fechaApartadoActual) {
        super(parentView.getFrame(), "Editar Apartado", true);
        this.controller = controller;
        this.parentView = parentView;
        this.productoId = productoId;
        initialize(fechaApartadoActual);
    }

    private void initialize(String fechaApartadoActual) {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta: Fecha de Apartado (YYYY-MM-DD)
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblFecha = new JLabel("Fecha de Apartado (YYYY-MM-DD):");
        lblFecha.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblFecha, gbc);

        // JDateChooser para día/mes/año
        gbc.gridx = 1;
        dateChooserApartado = new JDateChooser();
        dateChooserApartado.setDateFormatString("yyyy-MM-dd");
        dateChooserApartado.setFont(new Font("Arial", Font.PLAIN, 14));

        // Si fechaApartadoActual no está vacía, tratar de parsearla y asignarla al dateChooser
        try {
            if (fechaApartadoActual != null && !fechaApartadoActual.trim().isEmpty()) {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(fechaApartadoActual);
                dateChooserApartado.setDate(date);
            }
        } catch (Exception e) {
            // Si falla, no asignamos nada
        }
        panel.add(dateChooserApartado, gbc);

        // Panel de botones
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(new Color(240, 240, 240));

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBackground(new Color(30, 136, 229));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btnGuardar.setOpaque(true);
        btnGuardar.setContentAreaFilled(true);
        addHoverEffect(btnGuardar);

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBackground(new Color(211, 47, 47));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btnCancelar.setOpaque(true);
        btnCancelar.setContentAreaFilled(true);
        addHoverEffect(btnCancelar);

        btnGuardar.addActionListener(e -> guardarEdicion());
        btnCancelar.addActionListener(e -> confirmarCierre());

        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parentView.getFrame());
        setResizable(false);
        setVisible(true);
    }

    /**
     * [MR-007 – OPC-B] Siempre retorna {@code true} porque el formulario se
     * abre con la fecha de apartado pre-cargada; cualquier cierre sin guardar
     * podría descartar cambios realizados por el usuario.
     *
     * @return {@code true} siempre.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return true;
    }

    private void guardarEdicion() {
        // Obtener la fecha del JDateChooser
        Date date = dateChooserApartado.getDate();
        if (date == null) {
            JOptionPane.showMessageDialog(this, "La fecha de apartado es obligatoria.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Convertir a cadena "yyyy-MM-dd"
        String nuevaFechaApartado = new SimpleDateFormat("yyyy-MM-dd").format(date);

        // Llamar al método del controlador para actualizar la fecha de apartado
        if (controller.editarApartado(productoId, nuevaFechaApartado)) {
            JOptionPane.showMessageDialog(this, "Apartado actualizado exitosamente.");
            parentView.actualizarTabla();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error al actualizar el apartado.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Método para agregar efecto hover a los botones (similar a otras vistas)
    private void addHoverEffect(final JButton button) {
        final Color normalBg = button.getBackground();
        final Color hoverBg = normalBg.brighter();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normalBg);
                button.setCursor(Cursor.getDefaultCursor());
            }
        });
    }
}
