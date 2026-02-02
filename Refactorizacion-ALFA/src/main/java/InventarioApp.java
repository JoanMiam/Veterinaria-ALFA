import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
import controller.InventarioController;
import model.InventarioDAO;
import view.InventarioView;

public class InventarioApp {
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Veterinaria ALFA");
        System.setProperty("apple.awt.application.appearance", "system");

        System.setProperty("apple.awt.UIElement", "false");

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf. Using default look and feel.");
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            InventarioDAO dao = new InventarioDAO();
            dao.crearBaseDeDatos(); 
            InventarioController controller = new InventarioController(dao);
            new InventarioView(controller);
        });
    }
}
