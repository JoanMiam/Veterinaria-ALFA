package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;

public class EditarVentaView extends FormularioConfirmableDialog {
    public EditarVentaView(JDialog parent, InventarioController controller, Object id, String nombre, String cantidad, String fecha) {
        super(parent, "Editar Venta", true);
        setResizable(false);
        setLocationRelativeTo(parent);

        // Depuración
        System.out.println("EditarVentaView -> ID: " + id + ", Nombre: " + nombre + ", Cantidad: " + cantidad + ", Fecha: " + fecha);

        // Verificar y corregir los valores si es necesario (lógica intacta)
        boolean cantidadEsNumero = cantidad.matches("\\d+");
        boolean fechaEsValida = fecha.matches("\\d{4}-\\d{2}-\\d{2}");

        String nombreCorrecto = cantidadEsNumero ? nombre : cantidad;
        String cantidadCorrecta = cantidadEsNumero ? cantidad : "0";
        String fechaCorrecta = fechaEsValida ? fecha : LocalDate.now().toString();

        // Panel principal con GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta: Nombre del Medicamento
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblNombre = new JLabel("Nombre del Medicamento:");
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblNombre, gbc);

        // Campo de texto: Nombre
        gbc.gridx = 1;
        JTextField txtNombre = new JTextField(nombreCorrecto, 15);
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(txtNombre, gbc);

        // Etiqueta: Cantidad Vendida
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblCantidad = new JLabel("Cantidad Vendida:");
        lblCantidad.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblCantidad, gbc);

        // JSpinner para la cantidad
        gbc.gridx = 1;
        int valorCantidad = 0;
        try {
            valorCantidad = Integer.parseInt(cantidadCorrecta);
            if (valorCantidad < 0) {
                valorCantidad = 0;
            }
        } catch (NumberFormatException ex) {
            // Si no es un número, se queda en 0
        }
        JSpinner spinnerCantidad = new JSpinner(new SpinnerNumberModel(valorCantidad, 0, null, 1));
        // Ajustar editor para tener un ancho similar a 15 columnas
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerCantidad.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(spinnerCantidad, gbc);

        // Etiqueta: Fecha de Venta
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel lblFecha = new JLabel("Fecha de Venta (YYYY-MM-DD):");
        lblFecha.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblFecha, gbc);

        // Reemplazamos el campo de texto por JDateChooser
        gbc.gridx = 1;
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setFont(new Font("Arial", Font.PLAIN, 14));
        // Si la fechaCorrecta se pudo parsear, la mostramos en el dateChooser
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date parsedDate = sdf.parse(fechaCorrecta);
            dateChooser.setDate(parsedDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mainPanel.add(dateChooser, gbc);

        // Panel de botones
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

        // Lógica de botones (sin cambios)
        btnCancelar.addActionListener(e -> confirmarCierre());
        btnGuardar.addActionListener(e -> {
            String nuevoNombre = txtNombre.getText().trim();
            // Obtenemos el valor del spinner en lugar de txtCantidad
            Object spinnerValue = spinnerCantidad.getValue();
            String nuevaCantidad = spinnerValue != null ? spinnerValue.toString() : "0";

            // Obtenemos la fecha del dateChooser
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
            // Convertimos la fecha a String (yyyy-MM-dd) para mantener la misma lógica
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

            // Validar el formato de la fecha (lógica original)
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

        // Agregar paneles al contenedor principal
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Ajustar el tamaño automáticamente
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * [MR-007 – OPC-B] Siempre retorna {@code true} porque el formulario se
     * abre con datos pre-cargados de la venta a editar; cualquier cierre sin
     * guardar podría descartar cambios realizados por el usuario.
     *
     * @return {@code true} siempre.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return true;
    }
}
