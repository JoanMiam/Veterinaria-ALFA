package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import controller.InventarioController;

public class ApartadosView extends JDialog {
    private InventarioController controller;
    private DefaultTableModel model;
    private JTable table;
    private JFrame parentFrame;
    private TableRowSorter<DefaultTableModel> rowSorter;
    // Renderer para efecto hover en las filas
    private HoverTableCellRenderer hoverRenderer;

    public ApartadosView(JFrame parent, InventarioController controller) {
        super(parent, "Apartados", true);
        this.controller = controller;
        this.parentFrame = parent;
        initialize();
    }

    private void initialize() {
        setSize(800, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parentFrame);

        model = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Existencias", "Lote", "Caducidad", "Fecha Entrada", "Fecha Apartado"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(50, 50, 50));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false); // Evitar reordenar columnas

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Configurar renderer para efecto hover en las filas
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

        rowSorter = new TableRowSorter<>(model);
        table.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Panel inferior con botones: Registrar, Eliminar, Refrescar, Exportar
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnRegistrar = createButton("Registrar Apartado");
        JButton btnEliminar = createButton("Eliminar"); // Se asigna el rojo desde createButton
        JButton btnRefrescar = createButton("Refrescar");
        JButton btnExportar = createButton("Exportar a Excel");

        buttonPanel.add(btnRegistrar);
        buttonPanel.add(btnEliminar);
        buttonPanel.add(btnRefrescar);
        buttonPanel.add(btnExportar);

        // Agregamos también el botón "Editar Apartado"
        JButton btnEditarApartado = createButton("Editar Apartado");
        buttonPanel.add(btnEditarApartado);

        add(buttonPanel, BorderLayout.SOUTH);

        // ActionListeners
        btnRegistrar.addActionListener(e -> openRegistrarApartado());
        btnEliminar.addActionListener(e -> eliminarApartado());
        btnRefrescar.addActionListener(e -> cargarDatos());
        // [MR-005 – OPC-A] Exporta apartados como HTML en lugar de CSV
        btnExportar.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar Reporte HTML de Apartados");
            fileChooser.setSelectedFile(new File("apartados.html"));
            int selection = fileChooser.showSaveDialog(this);
            if (selection == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                controller.exportarApartadosCSV(file);
            }
        });
        btnEditarApartado.addActionListener(e -> editarApartado());

        cargarDatos();
        setVisible(true);
    }

    // Abre la ventana para registrar un apartado
    private void openRegistrarApartado() {
        new RegistrarApartadoView(this, controller);
    }

    // Elimina uno o más apartados (actualiza la base de datos, por ejemplo, quitando el apartado)
    private void eliminarApartado() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Seleccione uno o más apartados para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de eliminar los apartados seleccionados?",
                "Confirmación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            boolean allSuccess = true;
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int modelRow = table.convertRowIndexToModel(selectedRows[i]);
                int id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
                if (!controller.eliminarApartado(id)) {
                    allSuccess = false;
                }
            }
            if (allSuccess) {
                JOptionPane.showMessageDialog(this,
                        selectedRows.length == 1
                                ? "Apartado eliminado correctamente."
                                : "Apartados eliminados correctamente.",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(this,
                        "Hubo errores al eliminar algunos apartados.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            cargarDatos();
        }
    }

    // Método para editar la fecha de apartado (muestra EditarApartadoView)
    private void editarApartado() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un apartado para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (table.getSelectedRowCount() > 1) {
            JOptionPane.showMessageDialog(this, "Selecciona solo un apartado para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
        String fechaApartado = model.getValueAt(modelRow, 6) != null
                ? model.getValueAt(modelRow, 6).toString()
                : "";
        new EditarApartadoView(this, controller, id, fechaApartado);
    }

    public void cargarDatos() {
        model.setRowCount(0);
        List<Object[]> apartados = controller.obtenerProductosApartados();
        for (Object[] prod : apartados) {
            model.addRow(prod);
        }
    }

    public void actualizarTabla() {
        cargarDatos();
    }

    public JFrame getFrame() {
        return parentFrame;
    }

    // Método para crear botones con el estilo del proyecto y efecto hover
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        // Si es "Eliminar", se asigna el color rojo; de lo contrario, azul
        if (text.equalsIgnoreCase("Eliminar")) {
            button.setBackground(new Color(211, 47, 47));
        } else {
            button.setBackground(new Color(30, 136, 229));
        }
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        addHoverEffect(button);
        return button;
    }

    // Agrega efecto hover a un botón
    private void addHoverEffect(final JButton button) {
        final Color normalBg = button.getBackground();
        final Color hoverBg;
        if (normalBg.getRed() == 211 && normalBg.getGreen() == 47 && normalBg.getBlue() == 47) {
            hoverBg = new Color(180, 30, 30);
        } else {
            hoverBg = normalBg.brighter();
        }
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

    private void configurarOrdenacion() {
        // Por ejemplo, comparar existencias (columna 2) numéricamente
        rowSorter.setComparator(2, (o1, o2) -> {
            try {
                return Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()));
            } catch (NumberFormatException e) {
                return 0;
            }
        });
    }

    public void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar archivo CSV");
        fileChooser.setSelectedFile(new File("inventario.csv"));
        int selection = fileChooser.showSaveDialog(parentFrame);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            controller.exportarInventarioCSV(file);
        }
    }

    private void openAgregarMedicamento() {
        new AgregarMedicamentoView(parentFrame, controller);
    }

    private void openVentas() {
        new VentasView(parentFrame, controller);
    }

    private void openApartados() {
        new ApartadosView(parentFrame, controller);
    }

    /**
     * Renderer para “iluminar” la fila donde está el mouse sin cambiar la selección real.
     * Además, para la columna "Caducidad" (índice 4):
     * - Se asume que en la BD está guardada como "yyyy-MM" y se parsea con YearMonth.
     * - Se marca con rojo fuerte si el producto ya caducó.
     * - Se marca con rojo claro si está próximo a caducar (dentro de 30 días).
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

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Columna 4 -> "Caducidad", con formato "yyyy-MM"
            if (column == 4 && value != null) {
                try {
                    DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
                    YearMonth cadYM = YearMonth.parse(value.toString(), ymFormatter);

                    YearMonth hoyYM = YearMonth.now();
                    LocalDate hoy = LocalDate.now();
                    LocalDate expiryDate = cadYM.plusMonths(1).atDay(1);
                    long diasHastaExpiry = ChronoUnit.DAYS.between(hoy, expiryDate);

                    if (hoyYM.isAfter(cadYM)) {
                        c.setBackground(new Color(255, 0, 0)); // Rojo fuerte (ya caducado)
                    } else if (diasHastaExpiry <= 30) {
                        c.setBackground(new Color(255, 153, 153)); // Rojo claro (próximo a caducar)
                    } else if (row == hoveredRow && !isSelected) {
                        c.setBackground(new Color(220, 240, 255)); // Hover
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    }
                } catch (Exception e) {
                    // Si falla el parseo o la columna no tiene el formato esperado
                    c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                }
            } else {
                // Para el resto de columnas, o si no se cumple la condición
                if (row == hoveredRow && !isSelected) {
                    c.setBackground(new Color(220, 240, 255));
                } else {
                    c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                }
            }
            return c;
        }
    }
}
