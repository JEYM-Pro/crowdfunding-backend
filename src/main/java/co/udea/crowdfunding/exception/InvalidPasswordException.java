package co.udea.crowdfunding.exception;

/** HU-S01: la contraseña de confirmación está vacía o es incorrecta. El mensaje es genérico a propósito. */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("No se pudo verificar tu identidad. Revisa la contraseña e inténtalo de nuevo.");
    }
}
