package co.udea.crowdfunding.exception;

/** La campaña no admite el cambio pedido por su estado o por tener aportes (HTTP 409). */
public class CampaignNotEditableException extends RuntimeException {

    public CampaignNotEditableException(String message) {
        super(message);
    }
}
