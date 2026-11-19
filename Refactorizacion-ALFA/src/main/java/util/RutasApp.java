package util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

/**
 * [FIX-009] Resuelve las rutas de los archivos de datos de la aplicación
 * (base de datos y preferencias) a partir de la ubicación del ejecutable,
 * no del directorio de trabajo.
 *
 * <p>Antes, la BD se abría en {@code ./db} relativo al directorio desde donde se
 * lanzaba la app; si se abría desde otro lugar (acceso directo, terminal) se creaba
 * una BD vacía y parecía que se habían perdido los datos.
 *
 * <p>Orden de resolución del directorio base:
 * <ol>
 *   <li>Propiedad del sistema {@code -Dalfa.home=<ruta>} (override explícito).</li>
 *   <li>Propiedad {@code jpackage.app-path}: carpeta del {@code .exe} cuando se
 *       instala con jpackage (el JAR vive en la subcarpeta {@code app/}).</li>
 *   <li>Carpeta del {@code .jar} en ejecución.</li>
 *   <li>Directorio de trabajo (ejecución desde el IDE o {@code mvn}).</li>
 * </ol>
 */
public final class RutasApp {

    /** Propiedad del sistema que fija manualmente el directorio base. */
    public static final String PROP_HOME = "alfa.home";

    static final String CARPETA_BD = "db";
    static final String NOMBRE_BD = "baseDeDatosInventario.db";
    static final String NOMBRE_CONFIG = "config.properties";

    private RutasApp() { }

    /** @return directorio base donde viven {@code db/} y {@code config.properties}. */
    public static Path directorioBase() {
        String home = System.getProperty(PROP_HOME);
        if (home != null && !home.isBlank()) {
            return Paths.get(home).toAbsolutePath();
        }
        String lanzador = System.getProperty("jpackage.app-path");
        if (lanzador != null && !lanzador.isBlank()) {
            Path carpeta = Paths.get(lanzador).toAbsolutePath().getParent();
            if (carpeta != null) {
                return carpeta;
            }
        }
        Path jar = ubicacionJar();
        if (jar != null && jar.getParent() != null) {
            return jar.getParent();
        }
        return directorioTrabajo();
    }

    /** @return ruta del archivo SQLite ({@code <base>/db/baseDeDatosInventario.db}). */
    public static Path archivoBaseDatos() {
        return directorioBase().resolve(CARPETA_BD).resolve(NOMBRE_BD);
    }

    /** @return ruta del archivo de preferencias ({@code <base>/config.properties}). */
    public static Path archivoConfiguracion() {
        return directorioBase().resolve(NOMBRE_CONFIG);
    }

    /**
     * Crea la carpeta {@code db/} si hace falta y devuelve la ruta de la BD.
     *
     * <p>Compatibilidad con versiones anteriores: si en el directorio base todavía
     * no hay BD pero sí existe una en {@code ./db} del directorio de trabajo (donde
     * la guardaban las versiones previas), se <b>copia</b> a la nueva ubicación.
     * El archivo original no se modifica ni se borra.
     *
     * @return ruta absoluta del archivo de base de datos.
     * @throws IOException si no se puede crear la carpeta o copiar la BD previa.
     */
    public static Path prepararBaseDatos() throws IOException {
        Path legado = directorioTrabajo().resolve(CARPETA_BD).resolve(NOMBRE_BD);
        return prepararBaseDatos(archivoBaseDatos(), legado);
    }

    static Path prepararBaseDatos(Path destino, Path legado) throws IOException {
        Path destinoAbs = destino.toAbsolutePath().normalize();
        Files.createDirectories(destinoAbs.getParent());
        if (Files.notExists(destinoAbs) && legado != null) {
            Path legadoAbs = legado.toAbsolutePath().normalize();
            if (!legadoAbs.equals(destinoAbs) && Files.isRegularFile(legadoAbs)) {
                Files.copy(legadoAbs, destinoAbs);
                Registro.de(RutasApp.class).info("Base de datos previa copiada de " + legadoAbs + " a " + destinoAbs);
            }
        }
        return destinoAbs;
    }

    static Path directorioTrabajo() {
        return Paths.get("").toAbsolutePath();
    }

    /** @return ruta del JAR en ejecución, o {@code null} si se ejecuta desde clases sueltas. */
    private static Path ubicacionJar() {
        try {
            CodeSource cs = RutasApp.class.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) {
                return null;
            }
            Path ruta = Paths.get(cs.getLocation().toURI());
            return Files.isRegularFile(ruta) ? ruta : null;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
