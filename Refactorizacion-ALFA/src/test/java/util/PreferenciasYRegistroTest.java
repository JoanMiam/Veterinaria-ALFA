package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PRU-010-04 — Preferencia de tema y formato del log (MR-010).
 */
@DisplayName("PRU-010-04 | PreferenciasTema y Registro (MR-010)")
class PreferenciasYRegistroTest {

    @TempDir
    Path tmp;

    @Test
    @DisplayName("TC-01: sin archivo de configuración el tema es claro")
    void tc01_temaPorDefecto() {
        assertEquals(PreferenciasTema.CLARO, PreferenciasTema.leer(tmp.resolve("no-existe.properties")));
    }

    @Test
    @DisplayName("TC-02: guardar y leer el tema oscuro; valores desconocidos se guardan como claro")
    void tc02_guardarYLeer() {
        Path archivo = tmp.resolve("config.properties");

        PreferenciasTema.guardar(archivo, "DARK");
        assertEquals(PreferenciasTema.OSCURO, PreferenciasTema.leer(archivo));

        PreferenciasTema.guardar(archivo, "azul");
        assertEquals(PreferenciasTema.CLARO, PreferenciasTema.leer(archivo));
    }

    @Test
    @DisplayName("TC-03: el log escribe fecha, nivel, origen y la traza de la excepción")
    void tc03_formatoDelLog() {
        LogRecord registro = new LogRecord(Level.SEVERE, "Fallo al guardar");
        registro.setLoggerName("alfa.InventarioDAO");
        registro.setThrown(new IllegalStateException("BD bloqueada"));

        String linea = new Registro.FormatoLinea().format(registro);

        assertTrue(linea.matches("(?s)\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} SEVERE InventarioDAO - Fallo al guardar.*"),
                linea);
        assertTrue(linea.contains("IllegalStateException: BD bloqueada"));
    }

    @Test
    @DisplayName("TC-04: configurarArchivo crea la carpeta de logs")
    void tc04_carpetaDeLogs() {
        Path logs = tmp.resolve("logs");

        assertTrue(Registro.configurarArchivo(logs));
        Registro.de(PreferenciasYRegistroTest.class).info("prueba");

        assertTrue(logs.toFile().isDirectory());
    }
}
