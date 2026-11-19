package view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-009-03 — Filtro de búsqueda seguro (FIX-009): el texto del usuario se trata
 * como literal y ya no lanza {@code PatternSyntaxException}.
 */
@DisplayName("PRU-009-03 | FiltrosTabla — búsqueda literal (FIX-009)")
class FiltrosTablaTest {

    private TableRowSorter<DefaultTableModel> sorterCon(String... nombres) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nombre"}, 0);
        int id = 1;
        for (String nombre : nombres) {
            model.addRow(new Object[]{id++, nombre});
        }
        return new TableRowSorter<>(model);
    }

    @Test
    @DisplayName("TC-01: caracteres especiales de regex no lanzan excepción y se buscan literalmente")
    void tc01_caracteresEspeciales_noTruenan() {
        TableRowSorter<DefaultTableModel> sorter = sorterCon("Amoxicilina (500mg)", "Paracetamol", "Vitamina C*");

        assertDoesNotThrow(() -> sorter.setRowFilter(FiltrosTabla.contieneTexto("(")));
        assertEquals(1, sorter.getViewRowCount(), "Solo la fila que contiene '(' literal");

        sorter.setRowFilter(FiltrosTabla.contieneTexto("C*"));
        assertEquals(1, sorter.getViewRowCount(), "'*' se busca como carácter, no como comodín");

        assertDoesNotThrow(() -> sorter.setRowFilter(FiltrosTabla.contieneTexto("[")));
        assertEquals(0, sorter.getViewRowCount());
    }

    @Test
    @DisplayName("TC-02: ignora mayúsculas, incluidas letras acentuadas")
    void tc02_ignoraMayusculas() {
        TableRowSorter<DefaultTableModel> sorter = sorterCon("Ácido Fólico", "Paracetamol");

        sorter.setRowFilter(FiltrosTabla.contieneTexto("ácido"));

        assertEquals(1, sorter.getViewRowCount());
    }

    @Test
    @DisplayName("TC-03: texto vacío o en blanco quita el filtro")
    void tc03_textoVacio_sinFiltro() {
        assertNull(FiltrosTabla.contieneTexto(""));
        assertNull(FiltrosTabla.contieneTexto("   "));
        assertNull(FiltrosTabla.contieneTexto(null));
    }

    @Test
    @DisplayName("TC-04: respeta la columna indicada")
    void tc04_filtraPorColumna() {
        TableRowSorter<DefaultTableModel> sorter = sorterCon("Producto 2", "Otro");

        sorter.setRowFilter(FiltrosTabla.contieneTexto("2", 0)); // solo columna ID

        assertEquals(1, sorter.getViewRowCount(), "Solo el ID 2 coincide en la columna 0");
    }
}
