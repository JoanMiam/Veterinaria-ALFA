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
import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import controller.InventarioController;

public class RegistrarApartadoView extends FormularioConfirmableDialog {
    private JTextField txtSearch;
    private JDateChooser dateChooserApartado;
    private InventarioController controller;
    private ApartadosView parentView;

    public RegistrarApartadoView(ApartadosView parentView, InventarioController controller) {
        super(parentView.getFrame(), "Registrar Apartado", true);
        this.controller = controller;
        this.parentView = parentView;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        // Panel principal con GridBagLayout y fondo claro
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Etiqueta y campo para "Nombre o ID del Producto"
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblSearch = new JLabel("Nombre o ID del Producto:");
        lblSearch.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblSearch, gbc);

        gbc.gridx = 1;
        txtSearch = new JTextField(15);
        txtSearch.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(txtSearch, gbc);

        // Etiqueta y campo para "Fecha de Apartado"
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblFecha = new JLabel("Fecha de Apartado (YYYY-MM-DD):");
        lblFecha.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblFecha, gbc);

        gbc.gridx = 1;
        dateChooserApartado = new JDateChooser();
        dateChooserApartado.setDateFormatString("yyyy-MM-dd");
        dateChooserApartado.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(dateChooserApartado, gbc);

        // Panel de botones con estilo consistente
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

        btnGuardar.addActionListener(e -> registrarApartado());
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

    private void registrarApartado() {
        String search = txtSearch.getText().trim();
        Date date = dateChooserApartado.getDate();
        if (search.isEmpty() || date == null) {
            JOptionPane.showMessageDialog(this, "El campo de búsqueda y la fecha son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String fechaApartado = new SimpleDateFormat("yyyy-MM-dd").format(date);

        int id = -1;
        try {
            // Si el campo es numérico, se interpreta como ID
            id = Integer.parseInt(search);
        } catch (NumberFormatException e) {
            // Si no es numérico, se busca el producto por nombre
            id = controller.obtenerProductoIdPorNombre(search);
        }

        if (id == -1) {
            JOptionPane.showMessageDialog(this, "No se encontró el producto con el dato proporcionado.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (controller.separarProducto(id, fechaApartado)) {
            JOptionPane.showMessageDialog(this, "Producto apartado exitosamente.");
            parentView.actualizarTabla();
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error al registrar el apartado.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Agrega efecto hover a un botón: al entrar se aclara el color, al salir se restablece
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
