import javax.swing.SwingUtilities;
import controller.InventarioController;
import model.InventarioDAO;
import view.InventarioView;

public class InventarioApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventarioDAO dao = new InventarioDAO();
            dao.crearBaseDeDatos(); // Crea las tablas si no existen
            InventarioController controller = new InventarioController(dao);
            new InventarioView(controller);
        });
    }
}

