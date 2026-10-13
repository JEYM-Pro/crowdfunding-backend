package co.udea.crowdfunding.exception;

/** El usuario autenticado no es el creador de la campaña (HTTP 403). */
public class CampaignAccessDeniedException extends RuntimeException {

    public CampaignAccessDeniedException() {
        super("No tienes permiso para editar esta campaña.");
    }
}
