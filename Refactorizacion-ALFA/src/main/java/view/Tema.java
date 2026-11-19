package view;

import java.awt.Window;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import util.PreferenciasTema;
import util.Registro;

/**
 * [MR-006 / MR-010] Aplica el Look &amp; Feel FlatLaf claro u oscuro.
 */
public final class Tema {

    private static final Logger LOG = Registro.de(Tema.class);

    private Tema() { }

    /** Aplica FlatDarkLaf si {@code tema} es "dark"; FlatLightLaf en cualquier otro caso. */
    public static void aplicar(String tema) {
        try {
            UIManager.setLookAndFeel(PreferenciasTema.esOscuro(tema) ? new FlatDarkLaf() : new FlatLightLaf());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo aplicar el tema " + tema, e);
        }
    }

    /**
     * Cambia el tema en caliente en todas las ventanas abiertas y guarda la preferencia.
     * [MR-010] Ya no llama a {@code pack()}, que encogía la ventana principal.
     */
    public static void cambiar(String tema) {
        aplicar(tema);
        for (Window ventana : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(ventana);
            ventana.repaint();
        }
        PreferenciasTema.guardar(tema);
    }
}
