package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import controller.InventarioController;
import model.Producto;

/**
 * Productos apartados: registrar, editar fecha, eliminar apartado y exportar.
 */
public class ApartadosView extends JDialog {

    private static final long serialVersionUID = 1L;

    private final InventarioController controller;
    private final JFrame parentFrame;
    private DefaultTableModel model;
    private JTable table;
    /** Productos mostrados, en el mismo orden que las filas del modelo. */
    private List<Producto> apartados = new ArrayList<>();

    public ApartadosView(JFrame parent, InventarioController controller) {
        super(parent, "Apartados", true);
        this.controller = controller;
        this.parentFrame = parent;
        initialize();
    }

    private void initialize() {
        setSize(900, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parentFrame);

        model = Estilos.modeloSoloLectura("ID", "Nombre", "Existencias", "Lote", "Caducidad", "Fecha Entrada",
                "Fecha Apartado");
        table = new JTable(model);
        Estilos.configurarTabla(table);
        RendererTabla.instalar(table, InventarioView.COL_CADUCIDAD);
        TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(model);
        Tablas.ordenarComoNumero(rowSorter, InventarioView.COL_ID, InventarioView.COL_EXISTENCIAS);
        table.setRowSorter(rowSorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnRegistrar = Estilos.botonPrimario("Registrar Apartado");
        JButton btnEditar = Estilos.botonPrimario("Editar Apartado");
        JButton btnEliminar = Estilos.botonPeligro("Eliminar");
        JButton btnRefrescar = Estilos.botonPrimario("Refrescar");
        JButton btnExportar = Estilos.botonPrimario("Exportar reporte");
        btnRegistrar.addActionListener(e -> new RegistrarApartadoView(this, controller));
        btnEditar.addActionListener(e -> editarApartado());
        btnEliminar.addActionListener(e -> eliminarApartados());
        btnRefrescar.addActionListener(e -> cargarDatos());
        btnExportar.addActionListener(e -> Dialogos.exportarHtml(this, "Guardar Reporte HTML de Apartados",
                "apartados.html", controller::exportarApartados));

        JPanel botones = new JPanel(new FlowLayout());
        for (JButton b : List.of(btnRegistrar, btnEditar, btnEliminar, btnRefrescar, btnExportar)) {
            botones.add(b);
        }
        add(botones, BorderLayout.SOUTH);

        cargarDatos();
        setVisible(true);
    }

    /** Recarga los apartados desde la base de datos. */
    public void cargarDatos() {
        apartados = controller.obtenerProductosApartados();
        model.setRowCount(0);
        for (Producto p : apartados) {
            model.addRow(new Object[]{p.id(), p.nombre(), p.existencias(), p.lote(), p.caducidad(),
                    p.fechaEntrada(), p.fechaApartado()});
        }
    }

    public void actualizarTabla() {
        cargarDatos();
    }

    public JFrame getFrame() {
        return parentFrame;
    }

    private List<Producto> seleccionados() {
        List<Producto> lista = new ArrayList<>();
        for (int filaVista : table.getSelectedRows()) {
            lista.add(apartados.get(table.convertRowIndexToModel(filaVista)));
        }
        return lista;
    }

    private void editarApartado() {
        List<Producto> elegidos = seleccionados();
        if (elegidos.size() != 1) {
            Dialogos.aviso(this, "Selecciona un solo apartado para editar.");
            return;
        }
        new EditarApartadoView(this, controller, elegidos.get(0));
    }

    private void eliminarApartados() {
        List<Producto> elegidos = seleccionados();
        if (elegidos.isEmpty()) {
            Dialogos.aviso(this, "Seleccione uno o más apartados para eliminar.");
            return;
        }
        if (!Dialogos.confirmar(this, "¿Está seguro de eliminar los apartados seleccionados?")) {
            return;
        }
        List<String> errores = controller.eliminarApartados(elegidos.stream().map(Producto::id).toList());
        cargarDatos();
        Dialogos.resultadoLote(this, errores,
                elegidos.size() == 1 ? "Apartado eliminado correctamente." : "Apartados eliminados correctamente.");
    }
}
