package view;

import java.awt.Component;
import java.io.File;

import javax.swing.JOptionPane;

/**
 * [FIX-009] Mensajes al usuario tras generar un reporte. Antes los mostraba el DAO,
 * lo que impedía probarlo sin interfaz gráfica.
 */
final class MensajesExportacion {

    private MensajesExportacion() { }

    static void mostrarResultado(Component padre, boolean exito, File archivo) {
        if (exito) {
            JOptionPane.showMessageDialog(padre,
                    "Reporte exportado con éxito en:\n" + archivo.getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(padre,
                    "No se pudo exportar el reporte en:\n" + archivo.getAbsolutePath()
                            + "\nVerifique que la carpeta exista y que el archivo no esté abierto en otro programa.",
                    "Error al exportar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
