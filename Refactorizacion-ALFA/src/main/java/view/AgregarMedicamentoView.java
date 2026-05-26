package view;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import controller.InventarioController;

public class AgregarMedicamentoView extends FormularioConfirmableDialog {
    private JTextField txtNombre, txtLote;
    private JSpinner spinnerExistencias;
    // Reemplazamos el JDateChooser por dos combos:
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

        // Campo Nombre
        txtNombre = createTextField();
        // Spinner para existencias
        spinnerExistencias = new JSpinner(new SpinnerNumberModel(0, 0, null, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerExistencias.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));

        // Campo Lote
        txtLote = createTextField();

        // Combos para año y mes de caducidad
        comboAnio = new JComboBox<>();
        comboMes = new JComboBox<>();
        comboAnio.setFont(new Font("Arial", Font.PLAIN, 14));
        comboMes.setFont(new Font("Arial", Font.PLAIN, 14));

        // Rellenamos el combo de años (por ejemplo, desde el actual hasta 10 años adelante)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear; year <= currentYear + 10; year++) {
            comboAnio.addItem(year);
        }

        // Rellenamos el combo de meses (1 a 12)
        for (int month = 1; month <= 12; month++) {
            comboMes.addItem(month);
        }

        addLabeledField(gbc, 1, "Nombre:", txtNombre);
        addLabeledField(gbc, 2, "Existencias:", spinnerExistencias);
        addLabeledField(gbc, 3, "Lote:", txtLote);

        // Para caducidad, mostramos dos combos en la misma línea
        JPanel cadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cadPanel.setBackground(new Color(240, 240, 240));
        cadPanel.add(new JLabel("Año:"));
        cadPanel.add(comboAnio);
        cadPanel.add(new JLabel("Mes:"));
        cadPanel.add(comboMes);

        addLabeledField(gbc, 4, "Caducidad (YYYY-MM):", cadPanel);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(240, 240, 240));
        JButton btnGuardar = createStyledButton("Guardar", new Color(30, 136, 229));
        JButton btnCancelar = createStyledButton("Cancelar", new Color(211, 47, 47));
        btnGuardar.addActionListener(e -> guardarMedicamento());
        btnCancelar.addActionListener(e -> confirmarCierre());
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

    /**
     * [MR-007 – OPC-B] Retorna {@code true} si el usuario ingresó texto en
     * el campo Nombre o en el campo Lote, indicando que hay datos que se
     * perderían al cerrar sin guardar.
     *
     * @return {@code true} si {@code txtNombre} o {@code txtLote} contienen texto.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return !txtNombre.getText().trim().isEmpty() || !txtLote.getText().trim().isEmpty();
    }

    private void guardarMedicamento() {
        String nombre = txtNombre.getText().trim();
        String existencias = spinnerExistencias.getValue().toString().trim();
        String lote = txtLote.getText().trim();

        // Validar campos de texto vacíos
        if (nombre.isEmpty() || existencias.isEmpty() || lote.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Todos los campos son obligatorios.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Obtener año y mes de los combos
        Integer anioSeleccionado = (Integer) comboAnio.getSelectedItem();
        Integer mesSeleccionado = (Integer) comboMes.getSelectedItem();

        // Construimos "yyyy-MM"
        String caducidad = String.format("%04d-%02d", anioSeleccionado, mesSeleccionado);

        // Llamar al controlador
        if (controller.agregarProducto(nombre, existencias, lote, caducidad)) {
            JOptionPane.showMessageDialog(getParent(), "Medicamento agregado con éxito.");
            dispose();
        }
    }
}
