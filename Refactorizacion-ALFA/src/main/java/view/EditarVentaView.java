package view;

import com.toedter.calendar.JDateChooser;
import controller.InventarioController;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class EditarVentaView extends JDialog {
    public EditarVentaView(JDialog parent, InventarioController controller, Object id, String nombre, String cantidad, String fecha) {
        super(parent, "Editar Venta", true);
        setResizable(false);
        setLocationRelativeTo(parent);

        System.out.println("EditarVentaView -> ID: " + id + ", Nombre: " + nombre + ", Cantidad: " + cantidad + ", Fecha: " + fecha);

        boolean cantidadEsNumero = cantidad.matches("\\d+");
        boolean fechaEsValida = fecha.matches("\\d{4}-\\d{2}-\\d{2}");

        String nombreCorrecto = cantidadEsNumero ? nombre : cantidad;
        String cantidadCorrecta = cantidadEsNumero ? cantidad : "0";
        String fechaCorrecta = fechaEsValida ? fecha : LocalDate.now().toString();

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblNombre = new JLabel("Nombre del Medicamento:");
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblNombre, gbc);

        gbc.gridx = 1;
        JTextField txtNombre = new JTextField(nombreCorrecto, 15);
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(txtNombre, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblCantidad = new JLabel("Cantidad Vendida:");
        lblCantidad.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblCantidad, gbc);

        gbc.gridx = 1;
        int valorCantidad = 0;
        try {
            valorCantidad = Integer.parseInt(cantidadCorrecta);
            if (valorCantidad < 0) {
                valorCantidad = 0;
            }
        } catch (NumberFormatException ex) {
        }
        JSpinner spinnerCantidad = new JSpinner(new SpinnerNumberModel(valorCantidad, 0, null, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerCantidad.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(spinnerCantidad, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel lblFecha = new JLabel("Fecha de Venta (YYYY-MM-DD):");
        lblFecha.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblFecha, gbc);

        gbc.gridx = 1;
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setFont(new Font("Arial", Font.PLAIN, 14));
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedDate = sdf.parse(fechaCorrecta);
            dateChooser.setDate(parsedDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mainPanel.add(dateChooser, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 240, 240));

        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.setBackground(new Color(50, 143, 200));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setBackground(new Color(211, 47, 47));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFont(new Font("Arial", Font.BOLD, 14));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> {
            String nuevoNombre = txtNombre.getText().trim();
            Object spinnerValue = spinnerCantidad.getValue();
            String nuevaCantidad = spinnerValue != null ? spinnerValue.toString() : "0";

            Date selectedDate = dateChooser.getDate();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Todos los campos son obligatorios.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            String nuevaFecha = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);

            if (nuevoNombre.isEmpty() || nuevaCantidad.isEmpty() || nuevaFecha.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int cantidadNumerica = Integer.parseInt(nuevaCantidad);
                if (cantidadNumerica <= 0) {
                    JOptionPane.showMessageDialog(this, "La cantidad debe ser un número mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                java.time.LocalDate.parse(nuevaFecha, formatter);
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "La fecha debe tener el formato YYYY-MM-DD y ser una fecha válida.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                if (controller.editarVenta(id, nuevoNombre, nuevaCantidad, nuevaFecha)) {
                    JOptionPane.showMessageDialog(parent, "Venta actualizada con éxito.");
                    if (parent instanceof VentasView) {
                        ((VentasView) parent).actualizarTabla();
                    }
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al actualizar la venta.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ocurrió un error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnCancelar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
