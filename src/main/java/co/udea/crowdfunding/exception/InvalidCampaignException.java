package co.udea.crowdfunding.exception;

/** Datos de campaña que incumplen una regla de negocio (HTTP 400). */
public class InvalidCampaignException extends RuntimeException {

    public InvalidCampaignException(String message) {
        super(message);
    }
}
