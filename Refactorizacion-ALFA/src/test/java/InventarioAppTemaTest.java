import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRU-006-01 — Pruebas Unitarias: leerTema y aplicarTema en InventarioApp (MR-006).
 *
 * <p>Verifica que {@link InventarioApp#leerTema()} retorne el valor correcto
 * leyendo {@code config.properties} del directorio de trabajo, y que
 * {@link InventarioApp#aplicarTema(String)} no lance excepción para los
 * valores válidos {@code "light"} y {@code "dark"}.
 *
 * <p>Cada prueba preserva el estado previo de {@code config.properties} y lo
 * restaura en {@code @AfterEach} para no alterar la preferencia del desarrollador.
 */
@DisplayName("PRU-006-01 | InventarioApp — leerTema y aplicarTema (MR-006)")
class InventarioAppTemaTest {

    private static final String CONFIG = "config.properties";
    private String contenidoOriginal = null;

    @BeforeEach
    void setUp() throws IOException {
        File f = new File(CONFIG);
        if (f.exists()) {
            contenidoOriginal = new String(Files.readAllBytes(f.toPath()));
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        File f = new File(CONFIG);
        if (contenidoOriginal != null) {
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(contenidoOriginal);
            }
        } else {
            f.delete();
        }
        contenidoOriginal = null;
    }

    // =========================================================================
    // TC-01: sin config.properties → retorna "light" por defecto
    // =========================================================================

    @Test
    @DisplayName("TC-01: leerTema sin config.properties retorna 'light' por defecto")
    void tc01_leerTema_sinArchivo_retornaLight() {
        new File(CONFIG).delete();

        assertEquals("light", InventarioApp.leerTema(),
            "Sin archivo de configuración debe retornar 'light' como valor por defecto");
    }

    // =========================================================================
    // TC-02: config.properties con theme=dark → retorna "dark"
    // =========================================================================

    @Test
    @DisplayName("TC-02: leerTema con theme=dark en config.properties retorna 'dark'")
    void tc02_leerTema_archivoConDark_retornaDark() throws IOException {
        Properties p = new Properties();
        p.setProperty("theme", "dark");
        try (FileOutputStream fos = new FileOutputStream(CONFIG)) {
            p.store(fos, null);
        }

        assertEquals("dark", InventarioApp.leerTema(),
            "Con theme=dark en config.properties debe retornar 'dark'");
    }

    // =========================================================================
    // TC-03: config.properties con theme=light → retorna "light"
    // =========================================================================

    @Test
    @DisplayName("TC-03: leerTema con theme=light en config.properties retorna 'light'")
    void tc03_leerTema_archivoConLight_retornaLight() throws IOException {
        Properties p = new Properties();
        p.setProperty("theme", "light");
        try (FileOutputStream fos = new FileOutputStream(CONFIG)) {
            p.store(fos, null);
        }

        assertEquals("light", InventarioApp.leerTema(),
            "Con theme=light en config.properties debe retornar 'light'");
    }

    // =========================================================================
    // TC-04: config.properties existe pero sin clave "theme" → retorna "light"
    // =========================================================================

    @Test
    @DisplayName("TC-04: leerTema con archivo sin clave 'theme' retorna 'light' por defecto")
    void tc04_leerTema_archivoSinClave_retornaLight() throws IOException {
        Properties p = new Properties();
        p.setProperty("otra_clave", "valor");
        try (FileOutputStream fos = new FileOutputStream(CONFIG)) {
            p.store(fos, null);
        }

        assertEquals("light", InventarioApp.leerTema(),
            "Si la clave 'theme' no está definida debe retornar 'light'");
    }

    // =========================================================================
    // TC-05: aplicarTema("light") no lanza excepción
    // =========================================================================

    @Test
    @DisplayName("TC-05: aplicarTema('light') aplica FlatLightLaf sin lanzar excepción")
    void tc05_aplicarTema_light_noLanzaExcepcion() {
        assertDoesNotThrow(() -> InventarioApp.aplicarTema("light"),
            "aplicarTema('light') no debe lanzar ninguna excepción");
    }

    // =========================================================================
    // TC-06: aplicarTema("dark") no lanza excepción
    // =========================================================================

    @Test
    @DisplayName("TC-06: aplicarTema('dark') aplica FlatDarkLaf sin lanzar excepción")
    void tc06_aplicarTema_dark_noLanzaExcepcion() {
        assertDoesNotThrow(() -> InventarioApp.aplicarTema("dark"),
            "aplicarTema('dark') no debe lanzar ninguna excepción");
    }
}
