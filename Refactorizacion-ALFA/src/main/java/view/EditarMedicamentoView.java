package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;

public class EditarMedicamentoView extends FormularioConfirmableDialog {

    public EditarMedicamentoView(JFrame parent,
                                 InventarioController controller,
                                 int id,
                                 String nombreActual,
                                 String existenciasActual,
                                 String loteActual,
                                 String caducidadActual,
                                 String fechaEntradaActual) {
        super(parent, "Editar Medicamento", true);
        setResizable(false);
        setLocationRelativeTo(parent);

        // Panel principal con GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta y campo: Nombre
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblNombre, gbc);

        gbc.gridx = 1;
        JTextField txtNombre = new JTextField(nombreActual, 15);
        txtNombre.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(txtNombre, gbc);

        // Etiqueta y campo: Existencias (spinner)
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblExistencias = new JLabel("Existencias:");
        lblExistencias.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblExistencias, gbc);

        gbc.gridx = 1;
        int valorExistencias = 0;
        try {
            valorExistencias = Integer.parseInt(existenciasActual);
            if (valorExistencias < 0) {
                valorExistencias = 0;
            }
        } catch (NumberFormatException ex) {
            // Si no es número, se queda en 0
        }
        JSpinner spinnerExistencias = new JSpinner(new SpinnerNumberModel(valorExistencias, 0, null, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinnerExistencias.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(spinnerExistencias, gbc);

        // Etiqueta y campo: Lote
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel lblLote = new JLabel("Lote:");
        lblLote.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblLote, gbc);

        gbc.gridx = 1;
        JTextField txtLote = new JTextField(loteActual, 15);
        txtLote.setFont(new Font("Arial", Font.PLAIN, 14));
        mainPanel.add(txtLote, gbc);

        // Etiqueta: Caducidad
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel lblCaducidad = new JLabel("Caducidad (YYYY-MM):");
        lblCaducidad.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblCaducidad, gbc);

        // Panel con combos para año/mes en lugar de JDateChooser
        gbc.gridx = 1;
        JPanel cadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cadPanel.setBackground(new Color(240, 240, 240));

        JComboBox<Integer> comboAnio = new JComboBox<>();
        JComboBox<Integer> comboMes = new JComboBox<>();
        comboAnio.setFont(new Font("Arial", Font.PLAIN, 14));
        comboMes.setFont(new Font("Arial", Font.PLAIN, 14));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int year = currentYear; year <= currentYear + 10; year++) {
            comboAnio.addItem(year);
        }
        for (int m = 1; m <= 12; m++) {
            comboMes.addItem(m);
        }

        // Si caducidadActual tiene algo, parsear "yyyy-MM" y setear combos
        if (caducidadActual != null && !caducidadActual.trim().isEmpty()) {
            try {
                DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
                YearMonth ym = YearMonth.parse(caducidadActual, ymFormatter);
                comboAnio.setSelectedItem(ym.getYear());
                comboMes.setSelectedItem(ym.getMonthValue());
            } catch (DateTimeParseException e) {
                // Si falla, no asignamos
            }
        }

        cadPanel.add(new JLabel("Año:"));
        cadPanel.add(comboAnio);
        cadPanel.add(new JLabel("Mes:"));
        cadPanel.add(comboMes);

        mainPanel.add(cadPanel, gbc);

        // Etiqueta y campo: Fecha de Entrada (mantiene JDateChooser con día)
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel lblFechaEntrada = new JLabel("Fecha de Entrada (YYYY-MM-DD):");
        lblFechaEntrada.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(lblFechaEntrada, gbc);

        gbc.gridx = 1;
        JDateChooser dateChooserEntrada = new JDateChooser();
        dateChooserEntrada.setDateFormatString("yyyy-MM-dd");
        dateChooserEntrada.setFont(new Font("Arial", Font.PLAIN, 14));
        try {
            Date dateEnt = new SimpleDateFormat("yyyy-MM-dd").parse(fechaEntradaActual);
            dateChooserEntrada.setDate(dateEnt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mainPanel.add(dateChooserEntrada, gbc);

        // Panel de botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setBackground(new Color(240, 240, 240));

        JButton btnGuardar = createStyledButton("Guardar");
        JButton btnCancelar = createStyledButton("Cancelar");
        btnCancelar.setBackground(new Color(211, 47, 47)); // Rojo

        btnCancelar.addActionListener(e -> confirmarCierre());

        // Acción de guardar
        btnGuardar.addActionListener(e -> {
            String nuevoNombre = txtNombre.getText().trim();
            Object spinnerValueObj = spinnerExistencias.getValue();
            String nuevasExistencias = spinnerValueObj != null ? spinnerValueObj.toString() : "0";
            String nuevoLote = txtLote.getText().trim();

            // Caducidad
            Integer anioSel = (Integer) comboAnio.getSelectedItem();
            Integer mesSel = (Integer) comboMes.getSelectedItem();
            if (anioSel == null || mesSel == null) {
                JOptionPane.showMessageDialog(this, "Fecha de caducidad no válida.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String nuevaCaducidad = String.format("%04d-%02d", anioSel, mesSel);

            // Fecha de Entrada
            Date selectedDateEnt = dateChooserEntrada.getDate();
            if (selectedDateEnt == null) {
                JOptionPane.showMessageDialog(this, "Fecha de entrada NO válida.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String nuevaFechaEntrada = new SimpleDateFormat("yyyy-MM-dd").format(selectedDateEnt);

            // Verificar campos obligatorios
            if (nuevoNombre.isEmpty() || nuevasExistencias.isEmpty() || nuevoLote.isEmpty()
                    || nuevaCaducidad.isEmpty() || nuevaFechaEntrada.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Llamar al método de edición (el controlador recarga la vista)
            if (controller.editarProducto(id, nuevoNombre, nuevasExistencias, nuevoLote, nuevaCaducidad, nuevaFechaEntrada)) {
                JOptionPane.showMessageDialog(parent, "Medicamento actualizado con éxito.");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error al actualizar el medicamento.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(btnPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * [MR-007 – OPC-B] Siempre retorna {@code true} porque el formulario se
     * abre con datos pre-cargados del medicamento a editar; cualquier cierre
     * sin guardar podría descartar cambios realizados por el usuario.
     *
     * @return {@code true} siempre.
     */
    @Override
    protected boolean tieneDatosIngresados() {
        return true;
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
}
