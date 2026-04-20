package view;

import controller.InventarioController;
import javax.swing.*;
import java.awt.*;

/**
 * [MR-002 – OPC-A] Diálogo para registrar una venta.
 *
 * <p>Reemplaza los campos separados {@code txtId} y {@code txtNombre} por un
 * único campo {@code txtBusqueda} que acepta el ID numérico o el nombre exacto
 * del medicamento. La resolución del producto se delega al controlador.
 */
public class RegistrarVentaView extends JDialog {
    /** [MR-002 – OPC-A] Campo unificado: acepta ID numérico o nombre exacto del medicamento. */
    private JTextField txtBusqueda;
    private JSpinner spinnerCantidad; // Reemplaza txtCantidad
    private final InventarioController controller;
    private final VentasView parentView;

    public RegistrarVentaView(VentasView parentView, InventarioController controller) {
        super(parentView, "Agregar Venta", true);
        this.controller = controller;
        this.parentView = parentView;
        initialize();
    }

    private void initialize() {
        // Ubicamos la ventana relativa a la vista padre
        setLocationRelativeTo(parentView.getParentFrame());

        // Panel principal con GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta y campo para el ID
        JLabel lblBusqueda = new JLabel("ID o Nombre del Medicamento:");
        lblBusqueda.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblBusqueda, gbc);

        txtBusqueda = new JTextField(15);
        txtBusqueda.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        panel.add(txtBusqueda, gbc);

        // Etiqueta y campo para la Cantidad
        JLabel lblCantidad = new JLabel("Cantidad Vendida:");
        lblCantidad.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblCantidad, gbc);

        // JSpinner para la cantidad
        // (valor inicial = 1, mínimo = 1, sin máximo, paso = 1)
        spinnerCantidad = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        // Ajustar el editor para que sea de 15 columnas
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerCantidad.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));

        gbc.gridx = 1;
        panel.add(spinnerCantidad, gbc);

        // Panel para botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(new Color(240, 240, 240));
        JButton btnGuardar = createStyledButton("Guardar");
        JButton btnCancelar = createStyledButton("Cancelar");
        btnCancelar.setBackground(new Color(211, 47, 47)); // Rojo

        btnGuardar.addActionListener(e -> registrarVenta());
        btnCancelar.addActionListener(e -> dispose());
        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        // Agregar los paneles al contenido del JDialog
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

    /**
     * [MR-002 – OPC-A] Procesa el registro de una venta usando el campo unificado.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Valida que {@code txtBusqueda} y la cantidad no estén vacíos.</li>
     *   <li>Llama a {@link controller.InventarioController#buscarProductoPorIdONombre(String)}
     *       con el texto ingresado.</li>
     *   <li>Si el resultado es {@code null}, muestra un mensaje de error y
     *       aborta sin registrar la venta.</li>
     *   <li>Extrae {@code id} y {@code nombre} del arreglo retornado y delega
     *       a {@link controller.InventarioController#registrarVenta(int, String, int)}.</li>
     * </ol>
     */
    private void registrarVenta() {
        String busqueda = txtBusqueda.getText().trim();
        Object spinnerValue = spinnerCantidad.getValue();
        String cantidadStr = spinnerValue != null ? spinnerValue.toString() : "";

        if (busqueda.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object[] producto = controller.buscarProductoPorIdONombre(busqueda);
        if (producto == null) {
            JOptionPane.showMessageDialog(this,
                "No se encontró ningún medicamento con el ID o nombre ingresado.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int id = (int) producto[0];
            String nombre = (String) producto[1];
            int cantidad = Integer.parseInt(cantidadStr);

            if (controller.registrarVenta(id, nombre, cantidad)) {
                JOptionPane.showMessageDialog(this, "Venta registrada con éxito.");
                parentView.actualizarTabla();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error al registrar la venta.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
