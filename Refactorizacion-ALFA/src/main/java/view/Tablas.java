package view;

import java.util.Comparator;

import javax.swing.table.TableRowSorter;

/**
 * [MR-010] Utilidades para tablas.
 */
final class Tablas {

    private Tablas() { }

    /**
     * Ordena las columnas indicadas como números (por defecto Swing las ordena como
     * texto: "10" quedaba antes que "9").
     */
    static void ordenarComoNumero(TableRowSorter<?> sorter, int... columnas) {
        Comparator<Object> numerico = Comparator.comparingDouble(Tablas::comoNumero);
        for (int columna : columnas) {
            sorter.setComparator(columna, numerico);
        }
    }

    private static double comoNumero(Object valor) {
        if (valor instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return valor == null ? Double.NEGATIVE_INFINITY : Double.parseDouble(valor.toString());
        } catch (NumberFormatException e) {
            return Double.NEGATIVE_INFINITY;
        }
    }
}
