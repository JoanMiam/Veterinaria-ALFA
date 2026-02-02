package view;

import controller.InventarioController;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;

public class VentasView extends JDialog {
    private InventarioController controller;
    private DefaultTableModel model;
    private JTable table;
    private JFrame parentFrame;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtFiltrar;
    private JComboBox<String> cmbFiltro;

    // Renderer personalizado para el “hover” en la tabla
    private HoverTableCellRenderer hoverRenderer;

    public VentasView(JFrame parent, InventarioController controller) {
        super(parent, "Historial de Ventas", true);
        this.controller = controller;
        this.parentFrame = parent;
        initialize();
    }

    private void initialize() {
        setSize(900, 600); // Aumentado para mejor visualización
        setLayout(new BorderLayout());
        setLocationRelativeTo(parentFrame);

        // Panel de filtros
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(new Color(240, 240, 240));
        filterPanel.add(new JLabel("Filtrar por:"));

        String[] criterios = { "ID Venta", "ID Producto", "Nombre", "Cantidad", "Fecha" };
        cmbFiltro = new JComboBox<>(criterios);
        filterPanel.add(cmbFiltro);

        txtFiltrar = new JTextField(20);
        txtFiltrar.setToolTipText("Ingrese el valor a filtrar...");
        filterPanel.add(txtFiltrar);

        add(filterPanel, BorderLayout.NORTH);

        // Modelo de tabla
        model = new DefaultTableModel(
                new String[] { "ID Venta", "ID Producto", "Nombre del Medicamento", "Cantidad Vendida",
                        "Fecha de Venta" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30); // Aumentado para mejor legibilidad
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(50, 50, 50));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        rowSorter = new TableRowSorter<>(model);
        table.setRowSorter(rowSorter);

        // Renderer para hover en la tabla
        hoverRenderer = new HoverTableCellRenderer();
        table.setDefaultRenderer(Object.class, hoverRenderer);

        // Detectar movimiento del mouse para resaltar la fila
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                hoverRenderer.setHoveredRow(row);
                table.repaint();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRenderer.setHoveredRow(-1);
                table.repaint();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Panel de botones - Mejorado con más espaciado
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12)); // Más espacio entre botones
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding alrededor
        buttonPanel.setBackground(new Color(240, 240, 240));

        // Creamos los botones con su color respectivo
        JButton btnAgregarVenta = createButton("Agregar Venta", new Color(30, 136, 229));
        JButton btnEditarVenta = createButton("Editar Venta", new Color(30, 136, 229));
        JButton btnEliminarVenta = createButton("Eliminar", new Color(211, 47, 47)); // Rojo
        JButton btnExportarReporte = createButton("Exportar Reporte", new Color(30, 136, 229));
        JButton btnRefrescar = createButton("Refrescar Tabla", new Color(30, 136, 229));

        buttonPanel.add(btnAgregarVenta);
        buttonPanel.add(btnEditarVenta);
        buttonPanel.add(btnEliminarVenta);
        buttonPanel.add(btnExportarReporte);
        buttonPanel.add(btnRefrescar);

        add(buttonPanel, BorderLayout.SOUTH);

        // Listeners de los botones
        btnAgregarVenta.addActionListener(e -> openRegistrarVenta());
        btnEditarVenta.addActionListener(e -> editarVenta());
        btnEliminarVenta.addActionListener(e -> eliminarVenta());
        btnExportarReporte.addActionListener(e -> exportarReporte());
        btnRefrescar.addActionListener(e -> actualizarTabla());

        // Filtro en tiempo real
        txtFiltrar.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltroAvanzado();
            }

            public void removeUpdate(DocumentEvent e) {
                aplicarFiltroAvanzado();
            }

            public void changedUpdate(DocumentEvent e) {
                aplicarFiltroAvanzado();
            }
        });

        // Cargar datos
        cargarVentas();
        setVisible(true);
    }

    /**
     * Crea un botón con un color base y hover effect.
     */
    private JButton createButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        // Color base
        button.setBackground(baseColor);

        // Efecto hover
        addHoverEffect(button);

        return button;
    }

    /**
     * Agrega efecto de hover a un botón, aclarando (o oscureciendo) el color.
     */
    private void addHoverEffect(final JButton button) {
        final Color normalBg = button.getBackground();
        // Elige si quieres aclarar o oscurecer
        final Color hoverBg = normalBg.brighter(); // o normalBg.darker()

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

    private void cargarVentas() {
        model.setRowCount(0);
        for (Object[] venta : controller.obtenerHistorialVentas()) {
            System.out.println("Cargando en tabla -> " + Arrays.toString(venta)); // Depuración
            if (venta.length == 5) {
                model.addRow(venta);
            } else {
                System.err.println("Error: Registro de venta con tamaño incorrecto -> " + Arrays.toString(venta));
            }
        }
    }

    private void aplicarFiltroAvanzado() {
        String texto = txtFiltrar.getText().trim();
        int columnIndex = cmbFiltro.getSelectedIndex();
        if (texto.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, columnIndex));
        }
    }

    private void openRegistrarVenta() {
        new RegistrarVentaView(this, controller);
    }

    private void editarVenta() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Selecciona una venta para editar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        Object idVenta = model.getValueAt(modelRow, 0);
        Object idProducto = model.getValueAt(modelRow, 1);
        String nombreMedicamento = model.getValueAt(modelRow, 2).toString();
        String cantidadVendida = model.getValueAt(modelRow, 3).toString();
        String fechaVenta = model.getValueAt(modelRow, 4).toString();

        System.out.println("ID Venta: " + idVenta + ", Nombre: " + nombreMedicamento + ", Cantidad: " + cantidadVendida
                + ", Fecha: " + fechaVenta);

        new EditarVentaView(this, controller, idVenta, nombreMedicamento, cantidadVendida, fechaVenta);
    }

    private void eliminarVenta() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Selecciona una o más ventas para eliminar.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String mensajeConfirmacion = (selectedRows.length == 1)
                ? "¿Estás seguro de eliminar esta venta?"
                : "¿Estás seguro de eliminar las ventas seleccionadas?";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                mensajeConfirmacion,
                "Confirmación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Recorremos en orden descendente para no alterar los índices
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelRow = table.convertRowIndexToModel(selectedRows[i]);
                Object id = model.getValueAt(modelRow, 0);

                if (!controller.eliminarVenta(id)) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Error al eliminar la venta con ID: " + id,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            actualizarTabla();

            String mensajeExito = (selectedRows.length == 1)
                    ? "Venta eliminada con éxito."
                    : "Ventas eliminadas con éxito.";

            JOptionPane.showMessageDialog(
                    this,
                    mensajeExito,
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportarReporte() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Reporte de Ventas");
        fileChooser.setSelectedFile(new File("reporte_ventas.csv"));
        int selection = fileChooser.showSaveDialog(this);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.append("ID Venta, ID Producto, Nombre del Medicamento, Cantidad Vendida, Fecha de Venta\n");
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object value = model.getValueAt(row, col);
                        writer.append(value != null ? value.toString() : "");
                        if (col < model.getColumnCount() - 1)
                            writer.append(",");
                    }
                    writer.append("\n");
                }
                writer.flush();
                JOptionPane.showMessageDialog(this, "Reporte exportado con éxito.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al exportar el reporte: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void actualizarTabla() {
        cargarVentas();
    }

    public JFrame getParentFrame() {
        return parentFrame;
    }

    /**
     * Renderer para “iluminar” la fila donde está el mouse en la tabla
     * sin cambiar la selección real.
     */
    private static class HoverTableCellRenderer extends DefaultTableCellRenderer {
        private int hoveredRow = -1;

        public void setHoveredRow(int row) {
            this.hoveredRow = row;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (row == hoveredRow && !isSelected) {
                setBackground(new Color(220, 240, 255)); // Color “hover”
            } else {
                setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }
            return this;
        }
    }
}
