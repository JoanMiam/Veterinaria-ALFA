package view;

import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.toedter.calendar.JDateChooser;

/**
 * [MR-010] Componentes y conversiones de fecha para los formularios.
 */
final class Fechas {

    private static final int ANIOS_ADELANTE = 10;

    private Fechas() { }

    /** @return selector de día con formato {@code yyyy-MM-dd}, inicializado en {@code valor} (puede ser null). */
    static JDateChooser selectorDia(LocalDate valor) {
        JDateChooser selector = new JDateChooser();
        selector.setDateFormatString("yyyy-MM-dd");
        selector.setFont(Estilos.fuente(Font.PLAIN, 14));
        if (valor != null) {
            selector.setDate(Date.from(valor.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        return selector;
    }

    /** @return la fecha elegida, o {@code null} si está vacío. */
    static LocalDate valor(JDateChooser selector) {
        Date fecha = selector.getDate();
        return fecha == null ? null : fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** @return la fecha {@code yyyy-MM-dd}, o {@code null} si es nula o inválida. */
    static LocalDate parsear(String texto) {
        try {
            return texto == null ? null : LocalDate.parse(texto.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Selector de mes de caducidad (combos de año y mes).
     *
     * <p>[MR-010] Corrige un bug: al editar un producto con caducidad de un año pasado,
     * ese año no estaba en la lista y al guardar se cambiaba en silencio al año actual.
     * Ahora el año del valor inicial siempre se incluye.
     */
    static final class SelectorCaducidad extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JComboBox<Integer> anio = new JComboBox<>();
        private final JComboBox<Integer> mes = new JComboBox<>();

        SelectorCaducidad(YearMonth inicial) {
            super(new FlowLayout(FlowLayout.LEFT, 5, 0));
            setOpaque(false);
            int actual = YearMonth.now().getYear();
            int desde = inicial != null ? Math.min(actual, inicial.getYear()) : actual;
            int hasta = Math.max(actual + ANIOS_ADELANTE, inicial != null ? inicial.getYear() : actual);
            for (int a = desde; a <= hasta; a++) {
                anio.addItem(a);
            }
            for (int m = 1; m <= 12; m++) {
                mes.addItem(m);
            }
            anio.setFont(Estilos.fuente(Font.PLAIN, 14));
            mes.setFont(Estilos.fuente(Font.PLAIN, 14));
            YearMonth valor = inicial != null ? inicial : YearMonth.now();
            anio.setSelectedItem(valor.getYear());
            mes.setSelectedItem(valor.getMonthValue());
            add(new JLabel("Año:"));
            add(anio);
            add(new JLabel("Mes:"));
            add(mes);
        }

        YearMonth getValor() {
            return YearMonth.of((Integer) anio.getSelectedItem(), (Integer) mes.getSelectedItem());
        }
    }
}
