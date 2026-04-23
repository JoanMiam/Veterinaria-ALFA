import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
import controller.InventarioController;
import model.InventarioDAO;
import view.InventarioView;

public class InventarioApp {
    public static void main(String[] args) {
        // Configurar propiedades específicas de macOS antes de iniciar la UI
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Veterinaria ALFA");
        System.setProperty("apple.awt.application.appearance", "system");

        // Habilitar soporte para pantallas Retina en macOS
        System.setProperty("apple.awt.UIElement", "false");

        // Configurar FlatLaf como Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            // Opcional: Para tema oscuro usa: UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf. Using default look and feel.");
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            InventarioDAO dao = new InventarioDAO();
            dao.crearBaseDeDatos(); // Crea las tablas si no existen
            InventarioController controller = new InventarioController(dao);
            new InventarioView(controller);
        });
    }
}
