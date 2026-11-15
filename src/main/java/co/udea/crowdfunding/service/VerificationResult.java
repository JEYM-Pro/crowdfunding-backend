package co.udea.crowdfunding.service;

import co.udea.crowdfunding.exception.VerificationLockedException;

import java.time.Duration;
import java.util.function.Supplier;

/** HU-S07: resultado de una verificación con límite de intentos. */
public record VerificationResult(Status status, Duration retryAfter) {

    public enum Status { OK, FAILED, LOCKED }

    public static VerificationResult ok() {
        return new VerificationResult(Status.OK, Duration.ZERO);
    }

    public static VerificationResult failed() {
        return new VerificationResult(Status.FAILED, Duration.ZERO);
    }

    public static VerificationResult locked(Duration retryAfter) {
        return new VerificationResult(Status.LOCKED, retryAfter);
    }

    /** Lanza el error genérico de la verificación si falló, o el de bloqueo si está bloqueada. */
    public void orThrow(Supplier<? extends RuntimeException> onFailure) {
        switch (status) {
            case OK -> { }
            case FAILED -> throw onFailure.get();
            case LOCKED -> throw new VerificationLockedException(retryAfter);
        }
    }
}
