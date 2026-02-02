package view;

import controller.InventarioController;
import javax.swing.*;
import java.awt.*;
import java.util.Calendar;

public class AgregarMedicamentoView extends JDialog {
    private JTextField txtNombre, txtLote;
    private JSpinner spinnerExistencias;
    private JComboBox<Integer> comboAnio;
    private JComboBox<Integer> comboMes;

    private InventarioController controller;

    public AgregarMedicamentoView(JFrame parent, InventarioController controller) {
        super(parent, "Agregar Medicamento", true);
        this.controller = controller;
        initialize(parent);
    }

    private void initialize(JFrame parent) {
        setLayout(new GridBagLayout());
        setResizable(false);
        getContentPane().setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Agregar Medicamento", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(new Color(30, 136, 229));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(title, gbc);
        gbc.gridwidth = 1;

        txtNombre = createTextField();
        spinnerExistencias = new JSpinner(new SpinnerNumberModel(0, 0, null, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerExistencias.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));

        txtLote = createTextField();

        comboAnio = new JComboBox<>();
        comboMes = new JComboBox<>();
        comboAnio.setFont(new Font("Arial", Font.PLAIN, 14));
        comboMes.setFont(new Font("Arial", Font.PLAIN, 14));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear; year <= currentYear + 10; year++) {
            comboAnio.addItem(year);
        }

        for (int month = 1; month <= 12; month++) {
            comboMes.addItem(month);
        }

        addLabeledField(gbc, 1, "Nombre:", txtNombre);
        addLabeledField(gbc, 2, "Existencias:", spinnerExistencias);
        addLabeledField(gbc, 3, "Lote:", txtLote);

        JPanel cadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cadPanel.setBackground(new Color(240, 240, 240));
        cadPanel.add(new JLabel("Año:"));
        cadPanel.add(comboAnio);
        cadPanel.add(new JLabel("Mes:"));
        cadPanel.add(comboMes);

        addLabeledField(gbc, 4, "Caducidad (YYYY-MM):", cadPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(240, 240, 240));
        JButton btnGuardar = createStyledButton("Guardar", new Color(30, 136, 229));
        JButton btnCancelar = createStyledButton("Cancelar", new Color(211, 47, 47));
        btnGuardar.addActionListener(e -> guardarMedicamento());
        btnCancelar.addActionListener(e -> dispose());
        buttonPanel.add(btnGuardar);
        buttonPanel.add(btnCancelar);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void addLabeledField(GridBagConstraints gbc, int yPos, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = yPos;
        gbc.gridwidth = 1;
        add(label, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(component, gbc);
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField(15);
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        return textField;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private void guardarMedicamento() {
        String nombre = txtNombre.getText().trim();
        String existencias = spinnerExistencias.getValue().toString().trim();
        String lote = txtLote.getText().trim();

        if (nombre.isEmpty() || existencias.isEmpty() || lote.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Todos los campos son obligatorios.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        Integer anioSeleccionado = (Integer) comboAnio.getSelectedItem();
        Integer mesSeleccionado = (Integer) comboMes.getSelectedItem();

        String caducidad = String.format("%04d-%02d", anioSeleccionado, mesSeleccionado);

        if (controller.agregarProducto(nombre, existencias, lote, caducidad)) {
            JOptionPane.showMessageDialog(getParent(), "Medicamento agregado con éxito.");
            dispose();
        }
    }
}
