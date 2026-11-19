package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.ReglasCaducidad.Estado;

/**
 * PRU-009-02 — Regla única de caducidad (FIX-009). Usa fechas fijas para cubrir
 * los bordes de mes sin depender del día en que se corren las pruebas.
 */
@DisplayName("PRU-009-02 | ReglasCaducidad — regla única de caducidad (FIX-009)")
class ReglasCaducidadTest {

    private static final int UMBRAL = ReglasCaducidad.DIAS_ALERTA;

    @Test
    @DisplayName("TC-01: el día en que expira (1° del mes siguiente) cuenta como CADUCADO, no como PRÓXIMO")
    void tc01_diaDeExpiracion_esCaducado() {
        // caducidad 2026-11 → expira el 2026-12-01
        assertEquals(Estado.CADUCADO, ReglasCaducidad.estado("2026-11", LocalDate.of(2026, 12, 1), UMBRAL));
    }

    @Test
    @DisplayName("TC-02: el último día del mes de caducidad todavía es PRÓXIMO (vendible)")
    void tc02_ultimoDiaDelMes_esProximo() {
        assertEquals(Estado.PROXIMO, ReglasCaducidad.estado("2026-11", LocalDate.of(2026, 11, 30), UMBRAL));
        assertFalse(ReglasCaducidad.estaCaducado(YearMonth.of(2026, 11), LocalDate.of(2026, 11, 30)));
    }

    @Test
    @DisplayName("TC-03: a exactamente 30 días es PRÓXIMO; a 31 días es VIGENTE")
    void tc03_bordeDelUmbral() {
        // expira 2026-12-01: 30 días antes = 2026-11-01, 31 días antes = 2026-10-31
        assertEquals(Estado.PROXIMO, ReglasCaducidad.estado("2026-11", LocalDate.of(2026, 11, 1), UMBRAL));
        assertEquals(Estado.VIGENTE, ReglasCaducidad.estado("2026-11", LocalDate.of(2026, 10, 31), UMBRAL));
    }

    @Test
    @DisplayName("TC-04: meses anteriores son CADUCADO")
    void tc04_mesesAnteriores_caducado() {
        assertEquals(Estado.CADUCADO, ReglasCaducidad.estado("2025-01", LocalDate.of(2026, 11, 19), UMBRAL));
        assertTrue(ReglasCaducidad.estaCaducado(YearMonth.of(2025, 1), LocalDate.of(2026, 11, 19)));
    }

    @Test
    @DisplayName("TC-05: textos nulos, vacíos o mal formados son INVALIDA")
    void tc05_textosInvalidos() {
        LocalDate hoy = LocalDate.of(2026, 11, 19);
        assertEquals(Estado.INVALIDA, ReglasCaducidad.estado(null, hoy, UMBRAL));
        assertEquals(Estado.INVALIDA, ReglasCaducidad.estado("", hoy, UMBRAL));
        assertEquals(Estado.INVALIDA, ReglasCaducidad.estado("2026-13", hoy, UMBRAL));
        assertEquals(Estado.INVALIDA, ReglasCaducidad.estado("11/2026", hoy, UMBRAL));
        assertEquals(Estado.INVALIDA, ReglasCaducidad.estado("2026-11-01", hoy, UMBRAL));
        assertNull(ReglasCaducidad.parsear("abc"));
    }
}
