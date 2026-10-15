package co.udea.crowdfunding.exception;

/**
 * No se pudo completar la confirmación del aporte (por ejemplo, el correo no está configurado o el
 * proveedor SMTP lo rechazó). Al ser una excepción runtime, revierte la transacción (HTTP 500).
 */
public class ContributionProcessingException extends RuntimeException {

    public ContributionProcessingException(String message) {
        super(message);
    }

    public ContributionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
