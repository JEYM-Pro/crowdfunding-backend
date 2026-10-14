package co.udea.crowdfunding.exception;

/** La campaña solicitada no existe (HTTP 404). */
public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException() {
        super("Campaña no encontrada.");
    }
}
