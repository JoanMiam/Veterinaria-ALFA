package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import controller.InventarioController;
import model.Venta;

/**
 * Historial de ventas: registrar, editar, eliminar (regresa stock) y exportar.
 */
public class VentasView extends JDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final JFrame parentFrame;
    private DefaultTableModel model;
    private JTable table;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField txtFiltrar;
    private JComboBox<String> cmbFiltro;
    /** Ventas mostradas, en el mismo orden que las filas del modelo. */
    private List<Venta> ventas = new ArrayList<>();

    public VentasView(JFrame parent, InventarioController controller) {
        super(parent, "Historial de Ventas", true);
        this.controller = controller;
        this.parentFrame = parent;
        initialize();
    }

    private void initialize() {
        setSize(800, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parentFrame);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Filtrar por:"));
        cmbFiltro = new JComboBox<>(new String[]{"ID Venta", "ID Producto", "Nombre", "Cantidad", "Fecha"});
        filtros.add(cmbFiltro);
        txtFiltrar = new JTextField(20);
        txtFiltrar.setToolTipText("Ingrese el valor a filtrar...");
        filtros.add(txtFiltrar);
        add(filtros, BorderLayout.NORTH);

        model = Estilos.modeloSoloLectura("ID Venta", "ID Producto", "Nombre del Medicamento", "Cantidad Vendida",
                "Fecha de Venta");
        table = new JTable(model);
        Estilos.configurarTabla(table);
        RendererTabla.instalar(table, RendererTabla.SIN_CADUCIDAD);
        rowSorter = new TableRowSorter<>(model);
        Tablas.ordenarComoNumero(rowSorter, 0, 1, 3);
        table.setRowSorter(rowSorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnAgregar = Estilos.botonPrimario("Agregar Venta");
        JButton btnEditar = Estilos.botonPrimario("Editar Venta");
        JButton btnEliminar = Estilos.botonPeligro("Eliminar");
        JButton btnExportar = Estilos.botonPrimario("Exportar Reporte");
        JButton btnRefrescar = Estilos.botonPrimario("Refrescar Tabla");
        btnAgregar.addActionListener(e -> new RegistrarVentaView(this, controller));
        btnEditar.addActionListener(e -> editarVenta());
        btnEliminar.addActionListener(e -> eliminarVentas());
        btnExportar.addActionListener(e -> Dialogos.exportarHtml(this, "Guardar Reporte HTML de Ventas",
                "reporte_ventas.html", controller::exportarVentas));
        btnRefrescar.addActionListener(e -> actualizarTabla());

        JPanel botones = new JPanel(new FlowLayout());
        for (JButton b : List.of(btnAgregar, btnEditar, btnEliminar, btnExportar, btnRefrescar)) {
            botones.add(b);
        }
        add(botones, BorderLayout.SOUTH);

        // Filtro en tiempo real; [FIX-009] el texto se busca literal.
        DocumentListener alEscribir = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltro(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltro(); }
        };
        txtFiltrar.getDocument().addDocumentListener(alEscribir);
        cmbFiltro.addActionListener(e -> aplicarFiltro());

        actualizarTabla();
        setVisible(true);
    }

    /** Recarga el historial desde la base de datos. */
    public void actualizarTabla() {
        ventas = controller.obtenerHistorialVentas();
        model.setRowCount(0);
        for (Venta v : ventas) {
            model.addRow(new Object[]{v.id(), v.idProducto(), v.nombre(), v.cantidad(), v.fecha()});
        }
    }

    public JFrame getParentFrame() {
        return parentFrame;
    }

    private void aplicarFiltro() {
        rowSorter.setRowFilter(FiltrosTabla.contieneTexto(txtFiltrar.getText(), cmbFiltro.getSelectedIndex()));
    }

    private List<Venta> seleccionadas() {
        List<Venta> lista = new ArrayList<>();
        for (int filaVista : table.getSelectedRows()) {
            lista.add(ventas.get(table.convertRowIndexToModel(filaVista)));
        }
        return lista;
    }

    private void editarVenta() {
        List<Venta> elegidas = seleccionadas();
        if (elegidas.size() != 1) {
            Dialogos.aviso(this, "Selecciona una sola venta para editar.");
            return;
        }
        new EditarVentaView(this, controller, elegidas.get(0));
    }

    private void eliminarVentas() {
        List<Venta> elegidas = seleccionadas();
        if (elegidas.isEmpty()) {
            Dialogos.aviso(this, "Selecciona una o más ventas para eliminar.");
            return;
        }
        String pregunta = (elegidas.size() == 1
                ? "¿Estás seguro de eliminar esta venta?"
                : "¿Estás seguro de eliminar las " + elegidas.size() + " ventas seleccionadas?")
                + "\nLa cantidad vendida regresará al inventario.";
        if (!Dialogos.confirmar(this, pregunta)) {
            return;
        }
        List<String> errores = controller.eliminarVentas(elegidas.stream().map(Venta::id).toList());
        actualizarTabla();
        Dialogos.resultadoLote(this, errores,
                elegidas.size() == 1 ? "Venta eliminada con éxito." : "Ventas eliminadas con éxito.");
    }
}
