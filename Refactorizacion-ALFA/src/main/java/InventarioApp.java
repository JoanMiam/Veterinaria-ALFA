import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import controller.InventarioController;
import model.InventarioDAO;
import view.InventarioView;

/**
 * Punto de entrada de ALFA-INVENTORY.
 *
 * <p>[MR-006 – OPC-A] Lee la preferencia de tema desde {@code config.properties}
 * y aplica FlatDarkLaf o FlatLightLaf antes de construir la UI. La preferencia
 * se actualiza en tiempo de ejecución desde {@link view.InventarioView}.
 */
public class InventarioApp {

    /**
     * [MR-006 – OPC-A] Ruta del archivo de configuración de preferencias de usuario.
     * Se ubica junto al JAR ejecutable (directorio de trabajo).
     */
    public static final String CONFIG_PATH = "config.properties";

    /**
     * [MR-006 – OPC-A] Lee la preferencia de tema y aplica el Look &amp; Feel
     * correspondiente antes de lanzar la interfaz gráfica.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        aplicarTema(leerTema());
        SwingUtilities.invokeLater(() -> {
            InventarioDAO dao = new InventarioDAO();
            dao.crearBaseDeDatos();
            InventarioController controller = new InventarioController(dao);
            new InventarioView(controller);
        });
    }

    /**
     * [MR-006 – OPC-A] Lee el valor {@code theme} de {@code config.properties}.
     * Si el archivo no existe o la lectura falla, retorna {@code "light"} por defecto.
     *
     * @return {@code "dark"} o {@code "light"}.
     */
    public static String leerTema() {
        Properties props = new Properties();
        File cfg = new File(CONFIG_PATH);
        if (cfg.exists()) {
            try (FileInputStream fis = new FileInputStream(cfg)) {
                props.load(fis);
            } catch (IOException ignored) { }
        }
        return props.getProperty("theme", "light");
    }

    /**
     * [MR-006 – OPC-A] Aplica {@link FlatDarkLaf} si {@code tema} es {@code "dark"},
     * {@link FlatLightLaf} en cualquier otro caso. Los errores se imprimen en stderr
     * sin interrumpir el arranque.
     *
     * @param tema Cadena leída desde {@code config.properties}.
     */
    public static void aplicarTema(String tema) {
        try {
            if ("dark".equalsIgnoreCase(tema)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
