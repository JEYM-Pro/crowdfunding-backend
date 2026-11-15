package co.udea.crowdfunding.exception;

/**
 * HU-S05: el PIN de la campaña o la contraseña de la cuenta están vacíos o son incorrectos al cerrar.
 * El mensaje es genérico a propósito: no revela cuál de las dos credenciales falló.
 */
public class InvalidCloseCredentialsException extends RuntimeException {

    public InvalidCloseCredentialsException() {
        super("No se pudieron verificar las credenciales. La campaña conserva su estado actual.");
    }
}
