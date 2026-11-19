package util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * [MR-010] Registro de eventos (logging) de la aplicación con {@code java.util.logging}.
 *
 * <p>Sustituye los {@code System.out.println} y {@code printStackTrace} dispersos: los
 * errores quedan en {@code <directorio base>/logs/alfa-N.log} (rotación de 5 archivos
 * de 1&nbsp;MB) y se pueden revisar cuando algo falla en la veterinaria.
 *
 * <p>Uso: {@code private static final Logger LOG = Registro.de(MiClase.class);}
 */
public final class Registro {

    /** Nombre del logger raíz de la aplicación. */
    public static final String RAIZ = "alfa";

    private static final int TAMANO_MAXIMO = 1_000_000;
    private static final int ARCHIVOS = 5;

    private static FileHandler archivo;

    private Registro() { }

    /** @return logger hijo de {@value #RAIZ} para la clase indicada. */
    public static Logger de(Class<?> clase) {
        return Logger.getLogger(RAIZ + "." + clase.getSimpleName());
    }

    /**
     * Envía los registros de la aplicación a {@code carpeta/alfa-N.log}. Idempotente.
     * Si la carpeta no se puede crear, se sigue registrando solo en consola.
     *
     * @param carpeta carpeta donde se guardan los archivos de log.
     * @return {@code true} si quedó configurado el archivo.
     */
    public static synchronized boolean configurarArchivo(Path carpeta) {
        if (archivo != null) {
            return true;
        }
        try {
            Files.createDirectories(carpeta);
            FileHandler handler = new FileHandler(
                    carpeta.resolve("alfa-%g.log").toString(), TAMANO_MAXIMO, ARCHIVOS, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new FormatoLinea());
            handler.setLevel(Level.INFO);
            Logger.getLogger(RAIZ).addHandler(handler);
            archivo = handler;
            return true;
        } catch (IOException | SecurityException e) {
            Logger.getLogger(RAIZ).log(Level.WARNING, "No se pudo crear el archivo de log en " + carpeta, e);
            return false;
        }
    }

    /** Formato de una línea: {@code 2026-11-19 08:30:00 SEVERE InventarioDAO - mensaje}. */
    static final class FormatoLinea extends Formatter {
        private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord registro) {
            String origen = registro.getLoggerName() == null ? "" : registro.getLoggerName();
            if (origen.startsWith(RAIZ + ".")) {
                origen = origen.substring(RAIZ.length() + 1);
            }
            StringBuilder sb = new StringBuilder()
                    .append(LocalDateTime.ofInstant(registro.getInstant(), ZoneId.systemDefault()).format(FECHA)).append(' ')
                    .append(registro.getLevel().getName()).append(' ')
                    .append(origen).append(" - ")
                    .append(formatMessage(registro))
                    .append(System.lineSeparator());
            if (registro.getThrown() != null) {
                StringWriter traza = new StringWriter();
                registro.getThrown().printStackTrace(new PrintWriter(traza));
                sb.append(traza);
            }
            return sb.toString();
        }
    }
}
