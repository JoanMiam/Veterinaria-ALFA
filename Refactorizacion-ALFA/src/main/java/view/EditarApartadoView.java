package view;

import com.toedter.calendar.JDateChooser;
import controller.InventarioController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditarApartadoView extends JDialog {
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblFecha = new JLabel("Fecha de Apartado (YYYY-MM-DD):");
        lblFecha.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblFecha, gbc);

        gbc.gridx = 1;
        dateChooserApartado = new JDateChooser();
        dateChooserApartado.setDateFormatString("yyyy-MM-dd");
        dateChooserApartado.setFont(new Font("Arial", Font.PLAIN, 14));

        try {
            if (fechaApartadoActual != null && !fechaApartadoActual.trim().isEmpty()) {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(fechaApartadoActual);
                dateChooserApartado.setDate(date);
            }
        } catch (Exception e) {
        }
        panel.add(dateChooserApartado, gbc);

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
        btnCancelar.addActionListener(e -> dispose());

        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parentView.getFrame());
        setResizable(false);
        setVisible(true);
    }

    private void guardarEdicion() {
        Date date = dateChooserApartado.getDate();
        if (date == null) {
            JOptionPane.showMessageDialog(this, "La fecha de apartado es obligatoria.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nuevaFechaApartado = new SimpleDateFormat("yyyy-MM-dd").format(date);

        if (controller.editarApartado(productoId, nuevaFechaApartado)) {
            JOptionPane.showMessageDialog(this, "Apartado actualizado exitosamente.");
            parentView.actualizarTabla();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error al actualizar el apartado.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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
