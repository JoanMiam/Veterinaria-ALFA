package model;

/**
 * [MR-010] Una operación no se realizó porque viola una regla de negocio (producto
 * caducado, sin existencias, dato obligatorio vacío, registro inexistente...).
 *
 * <p>El mensaje está redactado para mostrarse tal cual al usuario. Sustituye a los
 * {@code JOptionPane} que antes mostraba el DAO directamente.
 */
public class OperacionRechazadaException extends Exception {

    private static final long serialVersionUID = 1L;

    public OperacionRechazadaException(String mensajeParaUsuario) {
        super(mensajeParaUsuario);
    }
}
