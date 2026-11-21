package co.udea.crowdfunding.exception;

/**
 * HU-S04: la contraseña de campaña (PIN) está vacía o es incorrecta.
 * El mensaje es genérico a propósito: no revela si el PIN faltó o no coincidió.
 */
public class InvalidCampaignPinException extends RuntimeException {

    public InvalidCampaignPinException() {
        super("No se pudo verificar la contraseña de campaña. Revísala e inténtalo de nuevo.");
    }
}
