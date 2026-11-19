package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [MR-010] Lectura y guardado de la preferencia de tema ({@code theme=light|dark}) en
 * {@code config.properties}. Antes esta lógica estaba copiada en {@code InventarioApp}
 * y en {@code InventarioView}.
 */
public final class PreferenciasTema {

    public static final String CLARO = "light";
    public static final String OSCURO = "dark";
    private static final String CLAVE = "theme";
    private static final Logger LOG = Registro.de(PreferenciasTema.class);

    private PreferenciasTema() { }

    /** @return {@value #OSCURO} o {@value #CLARO} (por defecto si no hay archivo o clave). */
    public static String leer() {
        return leer(RutasApp.archivoConfiguracion());
    }

    static String leer(Path archivo) {
        Properties props = new Properties();
        if (Files.isRegularFile(archivo)) {
            try (InputStream in = Files.newInputStream(archivo)) {
                props.load(in);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "No se pudo leer " + archivo, e);
            }
        }
        return props.getProperty(CLAVE, CLARO);
    }

    /** Guarda la preferencia para el siguiente arranque. Los errores solo se registran. */
    public static void guardar(String tema) {
        guardar(RutasApp.archivoConfiguracion(), tema);
    }

    static void guardar(Path archivo, String tema) {
        Properties props = new Properties();
        props.setProperty(CLAVE, esOscuro(tema) ? OSCURO : CLARO);
        try (OutputStream out = Files.newOutputStream(archivo)) {
            props.store(out, "Preferencia de tema visual: light | dark");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo guardar la preferencia de tema en " + archivo, e);
        }
    }

    /** @return {@code true} si {@code tema} es {@value #OSCURO} (sin distinguir mayúsculas). */
    public static boolean esOscuro(String tema) {
        return OSCURO.equalsIgnoreCase(tema);
    }
}
