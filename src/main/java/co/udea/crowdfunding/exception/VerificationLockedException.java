package co.udea.crowdfunding.exception;

import java.time.Duration;

/**
 * HU-S07: la verificación está bloqueada por demasiados intentos fallidos (HTTP 429).
 * Informa el tiempo de espera sin revelar qué dato era incorrecto.
 */
public class VerificationLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public VerificationLockedException(Duration retryAfter) {
        super(buildMessage(retryAfter));
        this.retryAfterSeconds = Math.max(1, retryAfter.toSeconds());
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String buildMessage(Duration retryAfter) {
        long minutes = Math.max(1, (retryAfter.toSeconds() + 59) / 60);
        return "Demasiados intentos fallidos. Podrás intentarlo de nuevo en " + minutes
                + (minutes == 1 ? " minuto." : " minutos.");
    }
}
