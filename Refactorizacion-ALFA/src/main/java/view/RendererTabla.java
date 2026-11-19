package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import model.ReglasCaducidad;

/**
 * [MR-001 / MR-010] Renderer único para las tablas de la aplicación. Antes había tres
 * copias (inventario, ventas, apartados) y dos aplicaban la regla de caducidad distinto.
 *
 * <ul>
 *   <li>Ilumina la fila bajo el mouse sin cambiar la selección.</li>
 *   <li>Si se indica una columna de caducidad, la pinta de rojo fuerte (caducado) o
 *       rojo claro (próximo a caducar), según {@link ReglasCaducidad}. Se puede
 *       desactivar con {@link #setMostrarColores(boolean)} (accesibilidad).</li>
 * </ul>
 */
public class RendererTabla extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    /** Valor de {@code columnaCaducidad} para tablas sin columna de caducidad. */
    public static final int SIN_CADUCIDAD = -1;

    private final int columnaCaducidad;
    private int hoveredRow = -1;
    private boolean mostrarColores = true;

    /**
     * @param columnaCaducidad índice de la columna de caducidad en la vista, o {@link #SIN_CADUCIDAD}.
     */
    public RendererTabla(int columnaCaducidad) {
        this.columnaCaducidad = columnaCaducidad;
    }

    /**
     * Crea el renderer, lo asigna a {@code tabla} y registra el seguimiento del mouse.
     *
     * @return el renderer instalado.
     */
    public static RendererTabla instalar(JTable tabla, int columnaCaducidad) {
        RendererTabla renderer = new RendererTabla(columnaCaducidad);
        tabla.setDefaultRenderer(Object.class, renderer);
        tabla.setDefaultRenderer(Number.class, renderer);
        tabla.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                renderer.setHoveredRow(tabla.rowAtPoint(e.getPoint()));
                tabla.repaint();
            }
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                renderer.setHoveredRow(-1);
                tabla.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (tabla.rowAtPoint(e.getPoint()) == -1) {
                    tabla.clearSelection();
                }
            }
        });
        return renderer;
    }

    public void setHoveredRow(int row) {
        this.hoveredRow = row;
    }

    /** Activa o desactiva los colores de caducidad; el hover sigue funcionando. */
    public void setMostrarColores(boolean mostrar) {
        this.mostrarColores = mostrar;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Color fondoCaducidad = null;
        if (mostrarColores && column == columnaCaducidad && value != null) {
            ReglasCaducidad.Estado estado = ReglasCaducidad.estado(
                    value.toString(), LocalDate.now(), ReglasCaducidad.DIAS_ALERTA);
            if (estado == ReglasCaducidad.Estado.CADUCADO) {
                fondoCaducidad = Estilos.CADUCADO;
            } else if (estado == ReglasCaducidad.Estado.PROXIMO) {
                fondoCaducidad = Estilos.PROXIMO;
            }
        }

        if (fondoCaducidad != null) {
            c.setBackground(fondoCaducidad);
            c.setForeground(Color.BLACK); // contraste legible también en tema oscuro
        } else if (row == hoveredRow && !isSelected) {
            c.setBackground(Estilos.colorHover());
            c.setForeground(table.getForeground());
        } else {
            c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        }
        return c;
    }
}
