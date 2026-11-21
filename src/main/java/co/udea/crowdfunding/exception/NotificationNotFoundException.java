package co.udea.crowdfunding.exception;

/** La notificación no existe o pertenece a otro usuario (HTTP 404). */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException() {
        super("Notificación no encontrada.");
    }
}
