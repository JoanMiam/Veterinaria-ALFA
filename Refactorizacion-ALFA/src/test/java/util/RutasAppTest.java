package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PRU-009-04 — Rutas de datos independientes del directorio de trabajo (FIX-009).
 */
@DisplayName("PRU-009-04 | RutasApp — ubicación de BD y configuración (FIX-009)")
class RutasAppTest {

    @TempDir
    Path tmp;

    @AfterEach
    void limpiarPropiedad() {
        System.clearProperty(RutasApp.PROP_HOME);
    }

    @Test
    @DisplayName("TC-01: -Dalfa.home fija la carpeta de la BD y de config.properties")
    void tc01_overridePorPropiedad() {
        System.setProperty(RutasApp.PROP_HOME, tmp.toString());

        assertEquals(tmp.toAbsolutePath(), RutasApp.directorioBase());
        assertEquals(tmp.resolve("db").resolve("baseDeDatosInventario.db").toAbsolutePath(), RutasApp.archivoBaseDatos());
        assertEquals(tmp.resolve("config.properties").toAbsolutePath(), RutasApp.archivoConfiguracion());
    }

    @Test
    @DisplayName("TC-02: si no hay BD en la nueva ruta, copia la de ./db (versión anterior) sin borrar la original")
    void tc02_copiaBdLegada() throws IOException {
        Path legado = tmp.resolve("viejo").resolve("db").resolve("baseDeDatosInventario.db");
        Files.createDirectories(legado.getParent());
        Files.writeString(legado, "datos");
        Path destino = tmp.resolve("app").resolve("db").resolve("baseDeDatosInventario.db");

        Path resultado = RutasApp.prepararBaseDatos(destino, legado);

        assertEquals(destino.toAbsolutePath().normalize(), resultado);
        assertEquals("datos", Files.readString(destino), "Se copió el contenido");
        assertTrue(Files.exists(legado), "El archivo original se conserva");
    }

    @Test
    @DisplayName("TC-03: si ya hay BD en la nueva ruta, no se sobrescribe")
    void tc03_noSobrescribeBdExistente() throws IOException {
        Path legado = tmp.resolve("viejo").resolve("db").resolve("baseDeDatosInventario.db");
        Files.createDirectories(legado.getParent());
        Files.writeString(legado, "viejo");
        Path destino = tmp.resolve("app").resolve("db").resolve("baseDeDatosInventario.db");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, "actual");

        RutasApp.prepararBaseDatos(destino, legado);

        assertEquals("actual", Files.readString(destino));
    }

    @Test
    @DisplayName("TC-04: sin BD previa solo crea la carpeta db/")
    void tc04_sinLegado_creaCarpeta() throws IOException {
        Path destino = tmp.resolve("app").resolve("db").resolve("baseDeDatosInventario.db");

        RutasApp.prepararBaseDatos(destino, tmp.resolve("no-existe.db"));

        assertTrue(Files.isDirectory(destino.getParent()));
        assertFalse(Files.exists(destino), "La BD la crea SQLite al conectarse, no RutasApp");
    }
}
