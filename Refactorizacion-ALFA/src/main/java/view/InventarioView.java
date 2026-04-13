package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import controller.InventarioController;

public class InventarioView {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private InventarioController controller;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtBuscar;

    // Renderer personalizado para la “fila hover”
    private HoverTableCellRenderer hoverRenderer;

    public InventarioView(InventarioController controller) {
        this.controller = controller;
        controller.setInventarioView(this);
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Inventario Veterinaria");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 550);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        model = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Existencias", "Lote", "Caducidad", "Fecha Entrada"},
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
        table.getTableHeader().setReorderingAllowed(false); // Evitar que el usuario reordene las columnas
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Renderer personalizado para hover en la tabla
        hoverRenderer = new HoverTableCellRenderer();
        table.setDefaultRenderer(Object.class, hoverRenderer);

        // Listeners para “hover” en la tabla
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
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row == -1) {
                    table.clearSelection();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        rowSorter = new TableRowSorter<>(model);
        table.setRowSorter(rowSorter);
        configurarOrdenacion();

        // Cargar los datos iniciales
        cargarDatos();

        // Panel superior con campo de búsqueda
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtBuscar = new JTextField(20);
        txtBuscar.setToolTipText("Buscar por nombre, lote o caducidad...");
        JButton btnBuscar = createButton("Buscar", "");
        addHoverEffect(btnBuscar);
        leftPanel.add(new JLabel("Buscar:"));
        leftPanel.add(txtBuscar);
        leftPanel.add(btnBuscar);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JToggleButton btnToggleColores = new JToggleButton("Colores caducidad: ON");
        btnToggleColores.setSelected(true);
        btnToggleColores.setFont(new Font("Arial", Font.BOLD, 14));
        btnToggleColores.setFocusPainted(false);
        btnToggleColores.setBackground(new Color(30, 136, 229));
        btnToggleColores.setForeground(Color.WHITE);
        btnToggleColores.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        btnToggleColores.addActionListener(e -> {
            boolean activo = btnToggleColores.isSelected();
            btnToggleColores.setText(activo ? "Colores caducidad: ON" : "Colores caducidad: OFF");
            btnToggleColores.setBackground(activo ? new Color(30, 136, 229) : new Color(100, 100, 100));
            hoverRenderer.setMostrarColores(activo);
            table.repaint();
        });
        rightPanel.add(btnToggleColores);
        JButton btnRefrescar = createButton("Refrescar", "");
        addHoverEffect(btnRefrescar);
        rightPanel.add(btnRefrescar);
        btnRefrescar.addActionListener(e -> actualizarTabla());

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        // Panel inferior con botones
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton btnAgregar = createButton("Agregar", "");
        addHoverEffect(btnAgregar);
        JButton btnEliminar = createButton("Eliminar", "");
        btnEliminar.setBackground(new Color(211, 47, 47)); // Rojo
        addHoverEffect(btnEliminar);
        JButton btnEditar = createButton("Editar", "");
        addHoverEffect(btnEditar);
        JButton btnExportar = createButton("Exportar a Excel", "");
        addHoverEffect(btnExportar);
        JButton btnVentas = createButton("Ventas", "");
        addHoverEffect(btnVentas);
        JButton btnApartados = createButton("Apartados", "");
        addHoverEffect(btnApartados);

        buttonPanel.add(btnAgregar);
        buttonPanel.add(btnEliminar);
        buttonPanel.add(btnEditar);
        buttonPanel.add(btnExportar);
        buttonPanel.add(btnVentas);
        buttonPanel.add(btnApartados);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Listeners de botones
        btnAgregar.addActionListener(e -> openAgregarMedicamento());
        btnEliminar.addActionListener(e -> eliminarProducto());
        btnEditar.addActionListener(e -> editarProducto());
        btnExportar.addActionListener(e -> exportarCSV());
        btnVentas.addActionListener(e -> openVentas());
        btnApartados.addActionListener(e -> openApartados());

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                aplicarFiltro(txtBuscar.getText());
            }
        });

        frame.setVisible(true);

        // Alerta de caducidad después de mostrar la ventana
        SwingUtilities.invokeLater(() -> {
            List<Object[]> proximos = controller.obtenerMedicamentosProximosACaducar(30);
            List<Object[]> caducados = controller.obtenerMedicamentosCaducados();
            if (!proximos.isEmpty() || !caducados.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Medicamentos próximos a caducar: " + proximos.size() + "\n" +
                                "Medicamentos ya caducados: " + caducados.size(),
                        "Alerta de Caducidad",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
    }

    private JButton createButton(String text, String icon) {
        JButton button = new JButton(icon + " " + text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBackground(new Color(30, 136, 229));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return button;
    }

    private void addHoverEffect(final JButton button) {
        final Color normalBg = button.getBackground();
        final Color hoverBg;
        // Si el botón es rojo (Eliminar), usamos un hover con rojo más oscuro
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
        // Ordenar la columna 2 (Existencias) como entero
        rowSorter.setComparator(2, (o1, o2) -> {
            try {
                return Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()));
            } catch (NumberFormatException e) {
                return 0;
            }
        });
    }

    public void cargarDatos() {
        model.setRowCount(0);
        Object[][] productos = controller.obtenerProductos();
        for (Object[] prod : productos) {
            model.addRow(prod);
        }
    }

    private void aplicarFiltro(String criterio) {
        if (criterio.trim().isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + criterio));
        }
    }

    private void eliminarProducto() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(frame, "Selecciona un medicamento para eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String mensajeConfirmacion = (selectedRows.length == 1)
                ? "¿Estás seguro de eliminar este medicamento?"
                : "¿Estás seguro de eliminar los medicamentos seleccionados?";
        int confirm = JOptionPane.showConfirmDialog(frame, mensajeConfirmacion, "Confirmación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean allSuccess = true;
            for (int selectedRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(selectedRow);
                int id = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
                if (!controller.eliminarProducto(id)) {
                    allSuccess = false;
                }
            }
            if (allSuccess) {
                cargarDatos();
                String mensajeExito = (selectedRows.length == 1)
                        ? "Medicamento eliminado con éxito."
                        : "Medicamentos eliminados con éxito.";
                JOptionPane.showMessageDialog(frame, mensajeExito, "Eliminado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Error al eliminar alguno(s) medicamento(s).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editarProducto() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Selecciona un medicamento para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        int id = Integer.parseInt(table.getValueAt(selectedRow, 0).toString());
        String nombre = table.getValueAt(selectedRow, 1).toString();
        String existencias = table.getValueAt(selectedRow, 2).toString();
        String lote = table.getValueAt(selectedRow, 3).toString();
        String caducidad = table.getValueAt(selectedRow, 4).toString();
        String fechaEntrada = table.getValueAt(selectedRow, 5).toString();

        new EditarMedicamentoView(
                frame,
                controller,
                id,
                nombre,
                existencias,
                lote,
                caducidad,
                fechaEntrada
        );
    }

    private void exportarCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar archivo CSV");
        fileChooser.setSelectedFile(new File("inventario.csv"));
        int selection = fileChooser.showSaveDialog(frame);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            controller.exportarInventarioCSV(file);
        }
    }

    private void openAgregarMedicamento() {
        new AgregarMedicamentoView(frame, controller);
    }

    private void openVentas() {
        new VentasView(frame, controller);
    }

    private void openApartados() {
        new ApartadosView(frame, controller);
    }

    public void actualizarTabla() {
        cargarDatos();
    }

    public JFrame getFrame() {
        return frame;
    }

    /**
     * Renderer para “iluminar” la fila donde está el mouse sin cambiar la selección real.
     * Además, para la columna "Caducidad":
     * - Se asume que en la BD se guarda "yyyy-MM".
     * - Se marca con rojo fuerte si el producto ya caducó.
     * - Se marca con rojo claro si está próximo a caducar (dentro de 30 días).
     */
    private static class HoverTableCellRenderer extends DefaultTableCellRenderer {
        private int hoveredRow = -1;
        private boolean mostrarColores = true;

        public void setHoveredRow(int row) {
            this.hoveredRow = row;
        }

        public void setMostrarColores(boolean mostrar) {
            this.mostrarColores = mostrar;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Columna 4 -> "Caducidad" con formato "yyyy-MM"
            if (column == 4 && value != null) {
                try {
                    // Parseamos como YearMonth
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                    // Convertir a YearMonth
                    YearMonth cadYM = YearMonth.parse(value.toString(), formatter);
                    LocalDate primerDiaCaducidad = cadYM.atDay(1);
                    LocalDate hoy = LocalDate.now();
                    long diasRestantes = ChronoUnit.DAYS.between(hoy, primerDiaCaducidad);

                    if (mostrarColores &&diasRestantes < 0) {
                        c.setBackground(new Color(255, 0, 0)); // Rojo fuerte (ya caducado)
                    } else if (mostrarColores && diasRestantes <= 30) {
                        c.setBackground(new Color(255, 153, 153)); // Rojo claro (próximo a caducar)
                    } else if (row == hoveredRow && !isSelected) {
                        c.setBackground(new Color(220, 240, 255)); // Hover
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    }
                } catch (DateTimeParseException e) {
                    // Si no se puede parsear "yyyy-MM", se deja color normal
                    c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                }
            } else {
                // Hover en filas para el resto de columnas
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
