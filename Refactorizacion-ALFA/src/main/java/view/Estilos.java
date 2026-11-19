package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.FlatLaf;

/**
 * [MR-010] Estilo visual común de la aplicación. Antes cada vista tenía su propia copia
 * de {@code createButton}, {@code addHoverEffect}, colores y fuentes.
 *
 * <p>Los fondos de paneles y formularios ya no se fijan a gris claro: se usan los del
 * tema activo, para que el modo oscuro se vea completo.
 */
public final class Estilos {

    /** Azul de los botones principales. */
    public static final Color AZUL = new Color(30, 136, 229);
    /** Rojo de los botones de eliminar/cancelar. */
    public static final Color ROJO = new Color(211, 47, 47);
    /** Fondo de la celda de un producto caducado. */
    public static final Color CADUCADO = new Color(255, 0, 0);
    /** Fondo de la celda de un producto próximo a caducar. */
    public static final Color PROXIMO = new Color(255, 153, 153);

    private static final Color HOVER_CLARO = new Color(220, 240, 255);
    private static final Color HOVER_OSCURO = new Color(55, 72, 92);
    private static final Color ENCABEZADO = new Color(50, 50, 50);
    private static final String FUENTE = "Arial";

    private Estilos() { }

    /** @return fuente de la aplicación con el estilo y tamaño indicados. */
    public static Font fuente(int estilo, int tamano) {
        return new Font(FUENTE, estilo, tamano);
    }

    /** @return color de la fila bajo el mouse, según el tema activo. */
    public static Color colorHover() {
        return FlatLaf.isLafDark() ? HOVER_OSCURO : HOVER_CLARO;
    }

    /** Botón azul con efecto hover. */
    public static JButton botonPrimario(String texto) {
        return boton(texto, AZUL);
    }

    /** Botón rojo (eliminar, cancelar) con efecto hover. */
    public static JButton botonPeligro(String texto) {
        return boton(texto, ROJO);
    }

    private static JButton boton(String texto, Color fondo) {
        JButton boton = new JButton(texto);
        aplicarEstiloBoton(boton, fondo);
        return boton;
    }

    /** Aplica el estilo de botón (también a {@code JToggleButton}). */
    public static void aplicarEstiloBoton(AbstractButton boton, Color fondo) {
        boton.setFont(fuente(Font.BOLD, 14));
        boton.setFocusPainted(false);
        boton.setBackground(fondo);
        boton.setForeground(Color.WHITE);
        boton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                boton.setBackground(ROJO.equals(fondo) ? new Color(180, 30, 30) : fondo.brighter());
                boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boton.setBackground(fondo);
                boton.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    /** Etiqueta en negritas para formularios. */
    public static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(fuente(Font.BOLD, 14));
        return etiqueta;
    }

    public static JTextField campoTexto(String valor) {
        JTextField campo = new JTextField(valor, 15);
        campo.setFont(fuente(Font.PLAIN, 14));
        return campo;
    }

    /** Spinner de enteros sin máximo. */
    public static JSpinner spinnerEntero(int valor, int minimo) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(valor, minimo), minimo, null, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setColumns(15);
        editor.getTextField().setFont(fuente(Font.PLAIN, 14));
        return spinner;
    }

    /** @return modelo de tabla de solo lectura con las columnas indicadas. */
    public static DefaultTableModel modeloSoloLectura(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    /** Estilo común de las tablas: alto de fila, fuentes, encabezado oscuro, selección múltiple. */
    public static void configurarTabla(JTable tabla) {
        tabla.setRowHeight(25);
        tabla.setFont(fuente(Font.PLAIN, 14));
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.getTableHeader().setFont(fuente(Font.BOLD, 16));
        tabla.getTableHeader().setBackground(ENCABEZADO);
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setGridColor(UIManager.getColor("Table.gridColor"));
    }
}
