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
import java.util.List;
import model.entities.Venta;

public class VentasView extends JDialog {
    private InventarioController controller;
    private DefaultTableModel model;
    private JTable table;
    private JFrame parentFrame;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtFiltrar;
    private JComboBox<String> cmbFiltro;

    private HoverTableCellRenderer hoverRenderer;

    public VentasView(JFrame parent, InventarioController controller) {
        super(parent, "Historial de Ventas", true);
        this.controller = controller;
        this.parentFrame = parent;
        initialize();
    }

    private void initialize() {
        setSize(900, 600); 
        setLayout(new BorderLayout());
        setLocationRelativeTo(parentFrame);

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
        table.setRowHeight(30); 
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(50, 50, 50));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        rowSorter = new TableRowSorter<>(model);
        table.setRowSorter(rowSorter);

        hoverRenderer = new HoverTableCellRenderer();
        table.setDefaultRenderer(Object.class, hoverRenderer);

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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12)); 
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        buttonPanel.setBackground(new Color(240, 240, 240));

        JButton btnAgregarVenta = createButton("Agregar Venta", new Color(30, 136, 229));
        JButton btnEditarVenta = createButton("Editar Venta", new Color(30, 136, 229));
        JButton btnEliminarVenta = createButton("Eliminar", new Color(211, 47, 47)); 
        JButton btnExportarReporte = createButton("Exportar Reporte", new Color(30, 136, 229));
        JButton btnRefrescar = createButton("Refrescar Tabla", new Color(30, 136, 229));

        buttonPanel.add(btnAgregarVenta);
        buttonPanel.add(btnEditarVenta);
        buttonPanel.add(btnEliminarVenta);
        buttonPanel.add(btnExportarReporte);
        buttonPanel.add(btnRefrescar);

        add(buttonPanel, BorderLayout.SOUTH);

        btnAgregarVenta.addActionListener(e -> openRegistrarVenta());
        btnEditarVenta.addActionListener(e -> editarVenta());
        btnEliminarVenta.addActionListener(e -> eliminarVenta());
        btnExportarReporte.addActionListener(e -> exportarReporte());
        btnRefrescar.addActionListener(e -> actualizarTabla());

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

        cargarVentas();
        setVisible(true);
    }

    private JButton createButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setBackground(baseColor);

        addHoverEffect(button);

        return button;
    }

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

    private void cargarVentas() {
        model.setRowCount(0);
        List<Venta> ventas = controller.obtenerHistorialVentas();
        for (Venta v : ventas) {
            model.addRow(new Object[] {
                    v.getId(), v.getIdProducto(), v.getNombre(),
                    v.getCantidad(), v.getFechaVenta()
            });
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
                setBackground(new Color(220, 240, 255)); 
            } else {
                setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            }
            return this;
        }
    }
}
