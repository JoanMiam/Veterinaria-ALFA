package view;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import model.OperacionRechazadaException;
import util.Registro;

/**
 * [MR-010] Mensajes al usuario. Centraliza los {@code JOptionPane} que antes estaban
 * repartidos en el DAO y en cada vista.
 */
final class Dialogos {

    private static final Logger LOG = Registro.de(Dialogos.class);

    private Dialogos() { }

    /** Acción de negocio que puede ser rechazada. */
    @FunctionalInterface
    interface Accion {
        void ejecutar() throws OperacionRechazadaException;
    }

    /** Acción que escribe un archivo. */
    @FunctionalInterface
    interface Exportacion {
        void escribir(File destino) throws IOException;
    }

    /**
     * Ejecuta {@code accion}; si se rechaza, muestra el motivo.
     *
     * @return {@code true} si la acción se completó.
     */
    static boolean ejecutar(Component padre, Accion accion) {
        try {
            accion.ejecutar();
            return true;
        } catch (OperacionRechazadaException e) {
            error(padre, e.getMessage());
            return false;
        }
    }

    static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** @return {@code true} si el usuario eligió "Sí". */
    static boolean confirmar(Component padre, String mensaje) {
        Object[] opciones = {"Sí", "No"};
        int opcion = JOptionPane.showOptionDialog(padre, mensaje, "Confirmación", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, opciones, opciones[1]);
        return opcion == 0;
    }

    /** Muestra el resultado de una operación por lotes (eliminar varios registros). */
    static void resultadoLote(Component padre, List<String> errores, String mensajeExito) {
        if (errores.isEmpty()) {
            info(padre, mensajeExito);
        } else {
            error(padre, "Algunos registros no se pudieron procesar:\n- " + String.join("\n- ", errores));
        }
    }

    /**
     * [MR-005] Pide dónde guardar un reporte HTML, lo genera e informa el resultado.
     */
    static void exportarHtml(Component padre, String titulo, String nombreSugerido, Exportacion exportacion) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle(titulo);
        selector.setSelectedFile(new File(nombreSugerido));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = selector.getSelectedFile();
        try {
            exportacion.escribir(destino);
            info(padre, "Reporte exportado con éxito en:\n" + destino.getAbsolutePath());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo exportar el reporte en " + destino, e);
            error(padre, "No se pudo exportar el reporte en:\n" + destino.getAbsolutePath()
                    + "\nVerifique que la carpeta exista y que el archivo no esté abierto en otro programa.");
        }
    }
}
