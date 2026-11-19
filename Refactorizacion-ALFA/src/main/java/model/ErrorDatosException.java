package model;

/**
 * [MR-010] Falla inesperada al leer o escribir la base de datos (archivo bloqueado,
 * disco lleno, BD dañada...). No es un error del usuario: se registra en el log y se
 * informa con un mensaje genérico.
 */
public class ErrorDatosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErrorDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
