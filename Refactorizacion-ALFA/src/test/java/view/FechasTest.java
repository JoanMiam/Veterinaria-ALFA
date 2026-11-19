package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import model.ReglasCaducidad;

/**
 * PRU-010-05 — Datos nulos o inválidos al abrir formularios de edición (MR-010).
 *
 * <p>Incluye el caso de #8 (41fa931): abrir "Editar" sobre un producto con caducidad
 * nula no debe fallar.
 */
@DisplayName("PRU-010-05 | Fechas — valores nulos e inválidos en formularios (MR-010)")
class FechasTest {

    @Test
    @DisplayName("TC-01: caducidad nula o inválida → el selector arranca en el mes actual (sin excepción)")
    void tc01_caducidadNula_selectorMesActual() {
        assertEquals(YearMonth.now(), new Fechas.SelectorCaducidad(ReglasCaducidad.parsear(null)).getValor());
        assertEquals(YearMonth.now(), new Fechas.SelectorCaducidad(ReglasCaducidad.parsear("basura")).getValor());
    }

    @Test
    @DisplayName("TC-02: una caducidad de un año pasado se conserva al abrir el formulario")
    void tc02_caducidadPasada_seConserva() {
        YearMonth pasada = YearMonth.now().minusYears(2).withMonth(3);

        assertEquals(pasada, new Fechas.SelectorCaducidad(pasada).getValor());
    }

    @Test
    @DisplayName("TC-03: fechas yyyy-MM-dd nulas o inválidas se interpretan como vacías")
    void tc03_fechasInvalidas() {
        assertNull(Fechas.parsear(null));
        assertNull(Fechas.parsear("2026-02-30"));
        assertNull(Fechas.parsear("hoy"));
        assertEquals(LocalDate.of(2026, 2, 3), Fechas.parsear(" 2026-02-03 "));
    }
}
