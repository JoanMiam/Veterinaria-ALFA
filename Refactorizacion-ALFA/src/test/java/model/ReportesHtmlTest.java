package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-010-03 — Generación de reportes HTML fuera del DAO (MR-005, FIX-009, MR-010).
 */
@DisplayName("PRU-010-03 | ReportesHtml — contenido y escape (MR-010)")
class ReportesHtmlTest {

    private static final Producto AMOXI = new Producto(1, "Amoxi <500mg> & \"Plus\"", 3, "L1", "2027-01",
            "2026-01-01", "2026-02-01");

    @Test
    @DisplayName("TC-01: escapa el texto de productos y de la clínica")
    void tc01_escapaTexto() {
        ConfiguracionVeterinaria config = new ConfiguracionVeterinaria(null, "Vet <ALFA>", null, null, null, null);

        String html = ReportesHtml.inventario(List.of(AMOXI), config);

        assertTrue(html.contains("Amoxi &lt;500mg&gt; &amp; &quot;Plus&quot;"));
        assertTrue(html.contains("Vet &lt;ALFA&gt;"));
        assertFalse(html.contains("<500mg>"));
    }

    @Test
    @DisplayName("TC-02: ventas sin fecha muestran N/A")
    void tc02_ventasSinFecha() {
        String html = ReportesHtml.ventas(List.of(new Venta(1, 1, "Med", 2, null)), null);

        assertTrue(html.contains("<td>N/A</td>"));
    }

    @Test
    @DisplayName("TC-03: apartados incluyen la fecha de apartado; sin configuración no hay encabezado de clínica")
    void tc03_apartados() {
        String html = ReportesHtml.apartados(List.of(AMOXI), null);

        assertTrue(html.contains("<td>2026-02-01</td>"));
        assertFalse(html.contains("vet-info\">"), "Sin configuración no se imprime el bloque de la clínica");
    }

    @Test
    @DisplayName("TC-04: el logotipo JPG se incrusta con su tipo MIME")
    void tc04_logoJpg() {
        ConfiguracionVeterinaria config = new ConfiguracionVeterinaria("/9j/AAAA", "Vet", null, null, null, null);

        assertTrue(ReportesHtml.inventario(List.of(), config).contains("data:image/jpeg;base64,/9j/AAAA"));
    }

    @Test
    @DisplayName("TC-05: escapar(null) devuelve cadena vacía")
    void tc05_escaparNull() {
        assertEquals("", ReportesHtml.escapar(null));
    }
}
