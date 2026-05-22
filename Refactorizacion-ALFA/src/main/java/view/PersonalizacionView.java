package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import controller.InventarioController;

/**
 * [MR-005 – OPC-A] Diálogo modal para configurar los datos de la clínica veterinaria
 * que aparecen en el encabezado de los reportes HTML.
 *
 * <p>Permite al usuario ingresar:
 * <ul>
 *   <li>Nombre de la clínica</li>
 *   <li>Dirección</li>
 *   <li>Teléfono</li>
 *   <li>RFC</li>
 *   <li>Horarios de atención</li>
 *   <li>Logotipo (imagen PNG/JPG convertida a Base64)</li>
 * </ul>
 *
 * <p>Los datos se persisten en la tabla {@code configuracion_veterinaria} de la base
 * de datos SQLite a través de {@link InventarioController#guardarConfiguracion}.
 * Al abrir el diálogo, se pre-cargan los valores existentes (si los hay) mediante
 * {@link InventarioController#obtenerConfiguracion}.
 */
public class PersonalizacionView extends JDialog {

    private final InventarioController controller;

    private JTextField txtNombre;
    private JTextField txtDireccion;
    private JTextField txtTelefono;
    private JTextField txtRfc;
    private JTextField txtHorarios;
    private JLabel lblLogoPreview;

    /** Base64 del logo seleccionado; {@code null} si no se cargó imagen. */
    private String logoBase64 = null;

    /**
     * [MR-005 – OPC-A] Construye y muestra el diálogo de personalización.
     *
     * @param parent     Ventana padre sobre la que se centra el diálogo.
     * @param controller Controlador que provee acceso a los métodos de personalización.
     */
    public PersonalizacionView(JFrame parent, InventarioController controller) {
        super(parent, "Personalización de la Veterinaria", true);
        this.controller = controller;
        initialize();
        cargarDatosExistentes();
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void initialize() {
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        txtNombre    = new JTextField(28);
        txtDireccion = new JTextField(28);
        txtTelefono  = new JTextField(28);
        txtRfc       = new JTextField(28);
        txtHorarios  = new JTextField(28);

        agregarFila(formPanel, gbc, 0, "Nombre de la veterinaria:", txtNombre);
        agregarFila(formPanel, gbc, 1, "Dirección:",                txtDireccion);
        agregarFila(formPanel, gbc, 2, "Teléfono:",                 txtTelefono);
        agregarFila(formPanel, gbc, 3, "RFC:",                      txtRfc);
        agregarFila(formPanel, gbc, 4, "Horarios:",                 txtHorarios);

        // Fila logo
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Logotipo:"), gbc);

        lblLogoPreview = new JLabel("Sin logo cargado");
        lblLogoPreview.setPreferredSize(new Dimension(80, 80));
        lblLogoPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblLogoPreview.setHorizontalAlignment(SwingConstants.CENTER);

        JButton btnSeleccionarLogo = new JButton("Seleccionar imagen...");
        btnSeleccionarLogo.addActionListener(e -> seleccionarLogo());

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.add(lblLogoPreview);
        logoPanel.add(Box.createHorizontalStrut(10));
        logoPanel.add(btnSeleccionarLogo);

        gbc.gridx = 1; gbc.gridy = 5;
        formPanel.add(logoPanel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnGuardar  = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");

        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());

        btnPanel.add(btnGuardar);
        btnPanel.add(btnCancelar);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void agregarFila(JPanel panel, GridBagConstraints gbc, int fila,
                              String label, JTextField campo) {
        gbc.gridx = 0; gbc.gridy = fila;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(campo, gbc);
    }

    /**
     * [MR-005 – OPC-A] Abre un selector de archivo filtrado a imágenes PNG/JPG,
     * lee los bytes del archivo elegido y los codifica en Base64. Muestra una
     * miniatura en {@link #lblLogoPreview}.
     */
    private void seleccionarLogo() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar logotipo");
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes (PNG, JPG)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File img = fc.getSelectedFile();
            try {
                byte[] bytes = Files.readAllBytes(img.toPath());
                logoBase64 = Base64.getEncoder().encodeToString(bytes);
                ImageIcon icon = new ImageIcon(bytes);
                Image scaled = icon.getImage().getScaledInstance(78, 78, Image.SCALE_SMOOTH);
                lblLogoPreview.setIcon(new ImageIcon(scaled));
                lblLogoPreview.setText("");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error al leer la imagen: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * [MR-005 – OPC-A] Valida que el nombre no esté vacío y persiste la configuración
     * invocando {@link InventarioController#guardarConfiguracion}. Cierra el diálogo
     * al completar el guardado.
     */
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de la veterinaria es obligatorio.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.guardarConfiguracion(
                logoBase64,
                nombre,
                txtDireccion.getText().trim(),
                txtTelefono.getText().trim(),
                txtRfc.getText().trim(),
                txtHorarios.getText().trim()
        );
        JOptionPane.showMessageDialog(this, "Configuración guardada correctamente.");
        dispose();
    }

    /**
     * [MR-005 – OPC-A] Recupera la configuración guardada y pre-puebla los campos
     * del formulario. Si no existe configuración, los campos quedan vacíos.
     */
    private void cargarDatosExistentes() {
        Object[] config = controller.obtenerConfiguracion();
        if (config == null) return;
        logoBase64 = config[0] != null ? config[0].toString() : null;
        if (logoBase64 != null && !logoBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(logoBase64);
                ImageIcon icon = new ImageIcon(bytes);
                Image scaled = icon.getImage().getScaledInstance(78, 78, Image.SCALE_SMOOTH);
                lblLogoPreview.setIcon(new ImageIcon(scaled));
                lblLogoPreview.setText("");
            } catch (Exception ignored) { }
        }
        if (config[1] != null) txtNombre.setText(config[1].toString());
        if (config[2] != null) txtDireccion.setText(config[2].toString());
        if (config[3] != null) txtTelefono.setText(config[3].toString());
        if (config[4] != null) txtRfc.setText(config[4].toString());
        if (config[5] != null) txtHorarios.setText(config[5].toString());
    }
}
