package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import controller.InventarioController;
import model.Producto;
import model.ReglasCaducidad;
import model.ReportesHtml;
import util.PreferenciasTema;

/**
 * Ventana principal: inventario de medicamentos.
 */
public class InventarioView {

    /** Índices de columna de la tabla. */
    static final int COL_ID = 0;
    static final int COL_EXISTENCIAS = 2;
    static final int COL_CADUCIDAD = 4;

    private static final int MAX_NOMBRES_ALERTA = 10;

    private final InventarioController controller;
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private RendererTabla renderer;
    /** Productos mostrados, en el mismo orden que las filas del modelo. */
    private List<Producto> productos = new ArrayList<>();

    public InventarioView(InventarioController controller) {
        this.controller = controller;
        // [MR-010] El controlador avisa los cambios; la vista ya no se registra a sí misma en él.
        controller.alCambiarInventario(this::cargarDatos);
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Inventario Veterinaria");
        // [MR-007 – OPC-B] Confirmación antes de cerrar el programa.
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Dialogos.confirmar(frame, "¿Está seguro de cerrar el programa?")) {
                    System.exit(0);
                }
            }
        });
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null);

        model = Estilos.modeloSoloLectura("ID", "Nombre", "Existencias", "Lote", "Caducidad", "Fecha Entrada");
        table = new JTable(model);
        Estilos.configurarTabla(table);
        renderer = RendererTabla.instalar(table, COL_CADUCIDAD);
        rowSorter = new TableRowSorter<>(model);
        Tablas.ordenarComoNumero(rowSorter, COL_ID, COL_EXISTENCIAS);
        table.setRowSorter(rowSorter);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        frame.add(crearPanelSuperior(), BorderLayout.NORTH);
        frame.add(crearPanelBotones(), BorderLayout.SOUTH);

        cargarDatos();
        frame.setVisible(true);
        SwingUtilities.invokeLater(this::mostrarAlertaCaducidad);
    }

    private JPanel crearPanelSuperior() {
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtBuscar = new JTextField(20);
        txtBuscar.setToolTipText("Buscar por nombre, lote o caducidad...");
        izquierda.add(new JLabel("Buscar:"));
        izquierda.add(txtBuscar);
        // [FIX-009] El texto se busca literal; [MR-010] filtra mientras se escribe.
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(txtBuscar.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(txtBuscar.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(txtBuscar.getText()); }
        });

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // [MR-001] Colores de caducidad desactivables (accesibilidad).
        JToggleButton btnColores = new JToggleButton("Colores caducidad: ON", true);
        Estilos.aplicarEstiloBoton(btnColores, Estilos.AZUL);
        btnColores.addActionListener(e -> {
            boolean activo = btnColores.isSelected();
            btnColores.setText(activo ? "Colores caducidad: ON" : "Colores caducidad: OFF");
            renderer.setMostrarColores(activo);
            table.repaint();
        });
        derecha.add(btnColores);

        // [MR-006] Tema claro / oscuro.
        boolean oscuro = PreferenciasTema.esOscuro(PreferenciasTema.leer());
        JToggleButton btnTema = new JToggleButton(oscuro ? "Tema: Oscuro" : "Tema: Claro", oscuro);
        Estilos.aplicarEstiloBoton(btnTema, Estilos.AZUL);
        btnTema.addActionListener(e -> {
            btnTema.setText(btnTema.isSelected() ? "Tema: Oscuro" : "Tema: Claro");
            Tema.cambiar(btnTema.isSelected() ? PreferenciasTema.OSCURO : PreferenciasTema.CLARO);
        });
        derecha.add(btnTema);

        JButton btnRefrescar = Estilos.botonPrimario("Refrescar");
        btnRefrescar.addActionListener(e -> cargarDatos());
        derecha.add(btnRefrescar);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(izquierda, BorderLayout.WEST);
        panel.add(derecha, BorderLayout.EAST);
        return panel;
    }

    private JPanel crearPanelBotones() {
        JButton btnAgregar = Estilos.botonPrimario("Agregar");
        JButton btnEliminar = Estilos.botonPeligro("Eliminar");
        JButton btnEditar = Estilos.botonPrimario("Editar");
        JButton btnExportar = Estilos.botonPrimario("Exportar reporte");
        JButton btnVentas = Estilos.botonPrimario("Ventas");
        JButton btnApartados = Estilos.botonPrimario("Apartados");
        JButton btnPersonalizacion = Estilos.botonPrimario("Personalización");

        btnAgregar.addActionListener(e -> new AgregarMedicamentoView(frame, controller));
        btnEliminar.addActionListener(e -> eliminarProductos());
        btnEditar.addActionListener(e -> editarProducto());
        btnExportar.addActionListener(e -> Dialogos.exportarHtml(frame, "Guardar Reporte HTML de Inventario",
                "inventario.html", controller::exportarInventario));
        btnVentas.addActionListener(e -> new VentasView(frame, controller));
        btnApartados.addActionListener(e -> new ApartadosView(frame, controller));
        btnPersonalizacion.addActionListener(e -> new PersonalizacionView(frame, controller));

        JPanel panel = new JPanel(new FlowLayout());
        for (JButton b : List.of(btnAgregar, btnEliminar, btnEditar, btnExportar, btnVentas, btnApartados,
                btnPersonalizacion)) {
            panel.add(b);
        }
        return panel;
    }

    /** Recarga la tabla desde la base de datos. */
    public void cargarDatos() {
        productos = controller.obtenerProductos();
        model.setRowCount(0);
        for (Producto p : productos) {
            model.addRow(new Object[]{p.id(), p.nombre(), p.existencias(), p.lote(), p.caducidad(), p.fechaEntrada()});
        }
    }

    public void actualizarTabla() {
        cargarDatos();
    }

    public JFrame getFrame() {
        return frame;
    }

    private void filtrar(String texto) {
        rowSorter.setRowFilter(FiltrosTabla.contieneTexto(texto));
    }

    private List<Producto> seleccionados() {
        List<Producto> lista = new ArrayList<>();
        for (int filaVista : table.getSelectedRows()) {
            lista.add(productos.get(table.convertRowIndexToModel(filaVista)));
        }
        return lista;
    }

    private void eliminarProductos() {
        List<Producto> elegidos = seleccionados();
        if (elegidos.isEmpty()) {
            Dialogos.aviso(frame, "Selecciona un medicamento para eliminar.");
            return;
        }
        String pregunta = (elegidos.size() == 1
                ? "¿Estás seguro de eliminar este medicamento?"
                : "¿Estás seguro de eliminar los " + elegidos.size() + " medicamentos seleccionados?")
                + "\nSu historial de ventas se conservará.";
        if (!Dialogos.confirmar(frame, pregunta)) {
            return;
        }
        List<String> errores = controller.eliminarProductos(elegidos.stream().map(Producto::id).toList());
        Dialogos.resultadoLote(frame, errores,
                elegidos.size() == 1 ? "Medicamento eliminado con éxito." : "Medicamentos eliminados con éxito.");
    }

    private void editarProducto() {
        List<Producto> elegidos = seleccionados();
        if (elegidos.size() != 1) {
            Dialogos.aviso(frame, "Selecciona un solo medicamento para editar.");
            return;
        }
        new EditarMedicamentoView(frame, controller, elegidos.get(0));
    }

    /** [MR-010] La alerta de caducidad ahora dice cuáles productos son, no solo cuántos. */
    private void mostrarAlertaCaducidad() {
        List<Producto> proximos = controller.obtenerMedicamentosProximosACaducar(ReglasCaducidad.DIAS_ALERTA);
        List<Producto> caducados = controller.obtenerMedicamentosCaducados();
        if (proximos.isEmpty() && caducados.isEmpty()) {
            return;
        }
        StringBuilder mensaje = new StringBuilder();
        if (!caducados.isEmpty()) {
            mensaje.append("Medicamentos ya caducados: ").append(caducados.size()).append('\n')
                   .append(resumen(caducados)).append('\n');
        }
        if (!proximos.isEmpty()) {
            mensaje.append("Medicamentos próximos a caducar (").append(ReglasCaducidad.DIAS_ALERTA)
                   .append(" días): ").append(proximos.size()).append('\n')
                   .append(resumen(proximos));
        }
        JLabel texto = new JLabel("<html>" + ReportesHtml.escapar(mensaje.toString().trim())
                .replace("\n", "<br>") + "</html>");
        texto.setFont(Estilos.fuente(Font.PLAIN, 13));
        JOptionPane.showMessageDialog(frame, texto, "Alerta de Caducidad",
                JOptionPane.WARNING_MESSAGE);
    }

    private static String resumen(List<Producto> lista) {
        String nombres = lista.stream()
                .limit(MAX_NOMBRES_ALERTA)
                .map(p -> "  • " + p.nombre() + " (lote " + p.lote() + ", " + p.caducidad() + ")")
                .collect(Collectors.joining("\n"));
        return lista.size() > MAX_NOMBRES_ALERTA
                ? nombres + "\n  … y " + (lista.size() - MAX_NOMBRES_ALERTA) + " más"
                : nombres;
    }
}
