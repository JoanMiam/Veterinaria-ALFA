package model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * [FIX-009] Única fuente de verdad para la regla de caducidad.
 *
 * <p>Antes la regla estaba copiada en el DAO y en dos renderers con criterios
 * distintos: el primer día del mes siguiente a la caducidad un producto contaba
 * a la vez como "próximo" y como "caducado", y cada tabla lo pintaba diferente.
 *
 * <p>Regla (MR-003): la caducidad se guarda como {@code yyyy-MM} y el producto
 * expira el <b>primer día del mes siguiente</b>. Con {@code dias = días hasta esa fecha}:
 * <ul>
 *   <li>{@code dias <= 0} → {@link Estado#CADUCADO}</li>
 *   <li>{@code 0 < dias <= umbral} → {@link Estado#PROXIMO}</li>
 *   <li>{@code dias > umbral} → {@link Estado#VIGENTE}</li>
 * </ul>
 */
public final class ReglasCaducidad {

    /** Días de anticipación con los que se alerta que un producto está por caducar. */
    public static final int DIAS_ALERTA = 30;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM");

    /** Estado de caducidad de un producto en una fecha dada. */
    public enum Estado { VIGENTE, PROXIMO, CADUCADO, INVALIDA }

    private ReglasCaducidad() { }

    /**
     * @param texto caducidad en formato {@code yyyy-MM}.
     * @return el {@link YearMonth}, o {@code null} si es nula, vacía o con formato inválido.
     */
    public static YearMonth parsear(String texto) {
        if (texto == null || !texto.matches("\\d{4}-\\d{2}")) {
            return null;
        }
        try {
            return YearMonth.parse(texto, FORMATO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** @return primer día en que el producto ya no se puede vender. */
    public static LocalDate fechaExpiracion(YearMonth caducidad) {
        return caducidad.plusMonths(1).atDay(1);
    }

    /** @return días que faltan para la fecha de expiración (negativo si ya pasó). */
    public static long diasRestantes(YearMonth caducidad, LocalDate hoy) {
        return ChronoUnit.DAYS.between(hoy, fechaExpiracion(caducidad));
    }

    /** @return {@code true} si en {@code hoy} el producto ya está caducado. */
    public static boolean estaCaducado(YearMonth caducidad, LocalDate hoy) {
        return diasRestantes(caducidad, hoy) <= 0;
    }

    /**
     * @param texto      caducidad en formato {@code yyyy-MM}.
     * @param hoy        fecha de referencia.
     * @param diasUmbral días de anticipación para considerar un producto próximo a caducar.
     * @return estado de caducidad; {@link Estado#INVALIDA} si el texto no se puede interpretar.
     */
    public static Estado estado(String texto, LocalDate hoy, int diasUmbral) {
        YearMonth caducidad = parsear(texto);
        if (caducidad == null) {
            return Estado.INVALIDA;
        }
        long dias = diasRestantes(caducidad, hoy);
        if (dias <= 0) {
            return Estado.CADUCADO;
        }
        return dias <= diasUmbral ? Estado.PROXIMO : Estado.VIGENTE;
    }
}
