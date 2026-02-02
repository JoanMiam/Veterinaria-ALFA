package view;

import controller.InventarioController;
import javax.swing.*;
import java.awt.*;

public class RegistrarVentaView extends JDialog {
    private JTextField txtId, txtNombre;
    private JSpinner spinnerCantidad;
    private InventarioController controller;
    private VentasView parentView;

    public RegistrarVentaView(VentasView parentView, InventarioController controller) {
        super(parentView, "Agregar Venta", true);
        this.controller = controller;
        this.parentView = parentView;
        initialize();
    }

    private void initialize() {
        setLocationRelativeTo(parentView.getParentFrame());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblId = new JLabel("ID del Medicamento:");
        lblId.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblId, gbc);

        txtId = new JTextField(15);
        txtId.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(txtId, gbc);

        JLabel lblNombre = new JLabel("Nombre del Medicamento:");
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(lblNombre, gbc);

        txtNombre = new JTextField(15);
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(txtNombre, gbc);

        JLabel lblCantidad = new JLabel("Cantidad Vendida:");
        lblCantidad.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblCantidad, gbc);

        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerCantidad.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));

        gbc.gridx = 1;
        panel.add(spinnerCantidad, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(new Color(240, 240, 240));
        JButton btnGuardar = createStyledButton("Guardar");
        JButton btnCancelar = createStyledButton("Cancelar");
        btnCancelar.setBackground(new Color(211, 47, 47)); 

        btnGuardar.addActionListener(e -> registrarVenta());
        btnCancelar.addActionListener(e -> dispose());
        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(btnPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(30, 136, 229));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private void registrarVenta() {
        String idStr = txtId.getText().trim();
        String nombre = txtNombre.getText().trim();
        Object spinnerValue = spinnerCantidad.getValue();
        String cantidadStr = spinnerValue != null ? spinnerValue.toString() : "";

        if (idStr.isEmpty() || nombre.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int id = Integer.parseInt(idStr);
            int cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (controller.registrarVenta(id, nombre, cantidad)) {
                JOptionPane.showMessageDialog(this, "Venta registrada con éxito.");
                parentView.actualizarTabla();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error al registrar la venta.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El ID y la cantidad deben ser números válidos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
