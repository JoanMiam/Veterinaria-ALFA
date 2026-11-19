import java.awt.GraphicsEnvironment;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import controller.InventarioController;
import model.InventarioDAO;
import util.PreferenciasTema;
import util.Registro;
import util.RutasApp;
import view.InventarioView;
import view.Tema;

/**
 * Punto de entrada de ALFA-INVENTORY.
 *
 * <p>[MR-006 – OPC-A] Lee la preferencia de tema desde {@code config.properties}
 * y aplica FlatDarkLaf o FlatLightLaf antes de construir la UI. La preferencia
 * se actualiza en tiempo de ejecución desde {@link view.InventarioView}.
 */
public class InventarioApp {

    private static final Logger LOG = Registro.de(InventarioApp.class);

    /**
     * [MR-006 – OPC-A] Ruta del archivo de configuración de preferencias de usuario.
     * [FIX-009] Se ubica junto al ejecutable, sin importar desde dónde se lance (ver {@link RutasApp}).
     */
    public static final String CONFIG_PATH = RutasApp.archivoConfiguracion().toString();

    /**
     * [MR-006 – OPC-A] Lee la preferencia de tema y aplica el Look &amp; Feel
     * correspondiente antes de lanzar la interfaz gráfica.
     *
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        // [MR-010] Errores y eventos quedan en <carpeta de la app>/logs/alfa-N.log
        Registro.configurarArchivo(RutasApp.directorioBase().resolve("logs"));
        instalarManejadorDeErrores();
        LOG.info("Iniciando ALFA-INVENTORY en " + RutasApp.directorioBase());

        aplicarTema(leerTema());
        SwingUtilities.invokeLater(() -> {
            InventarioDAO dao;
            try {
                // El constructor abre la BD junto al ejecutable y deja el esquema al día.
                dao = new InventarioDAO();
            } catch (IllegalStateException e) {
                // [FIX-009] Antes el error se imprimía y la app seguía con una conexión nula.
                LOG.log(Level.SEVERE, "No se pudo abrir la base de datos", e);
                JOptionPane.showMessageDialog(null,
                        "No se pudo abrir la base de datos:\n" + e.getMessage(),
                        "Error al iniciar", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            InventarioController controller = new InventarioController(dao);
            new InventarioView(controller);
        });
    }

    /**
     * [MR-010] Cualquier excepción no controlada (incluidas las del hilo de Swing) se
     * registra en el log y se avisa al usuario, en lugar de perderse en la consola.
     */
    static void instalarManejadorDeErrores() {
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            LOG.log(Level.SEVERE, "Error no controlado en el hilo " + hilo.getName(), error);
            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Ocurrió un error inesperado y la operación no se completó.\n"
                                + "El detalle quedó guardado en: " + RutasApp.directorioBase().resolve("logs"),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    /**
     * [MR-006 – OPC-A] Lee el valor {@code theme} de {@code config.properties}.
     * Si el archivo no existe o la lectura falla, retorna {@code "light"} por defecto.
     *
     * @return {@code "dark"} o {@code "light"}.
     */
    public static String leerTema() {
        return PreferenciasTema.leer();
    }

    /**
     * [MR-006 – OPC-A] Aplica {@link com.formdev.flatlaf.FlatDarkLaf} si {@code tema} es
     * {@code "dark"}, {@link com.formdev.flatlaf.FlatLightLaf} en cualquier otro caso.
     * Los errores se registran sin interrumpir el arranque.
     *
     * @param tema Cadena leída desde {@code config.properties}.
     */
    public static void aplicarTema(String tema) {
        Tema.aplicar(tema);
    }
}
