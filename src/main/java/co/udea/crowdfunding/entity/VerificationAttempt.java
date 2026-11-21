package co.udea.crowdfunding.entity;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * HU-S07: fallos consecutivos de un usuario en una verificación (contraseña de la cuenta o PIN de una
 * campaña) y, si se superó el límite, hasta cuándo queda bloqueada.
 */
@Entity
@Table(
    name = "verification_attempts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "scope"})
)
public class VerificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String scope;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VerificationAttempt() {
    }

    public VerificationAttempt(UUID userId, String scope, Instant now) {
        this.userId = userId;
        this.scope = scope;
        this.failedAttempts = 0;
        this.updatedAt = now;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Duration remainingLock(Instant now) {
        return isLockedAt(now) ? Duration.between(now, lockedUntil) : Duration.ZERO;
    }

    public boolean hasFailures() {
        return failedAttempts > 0 || lockedUntil != null;
    }

    public void registerFailure(Instant now) {
        this.failedAttempts++;
        this.updatedAt = now;
    }

    /** Bloquea la verificación; el contador vuelve a cero para que el siguiente ciclo empiece limpio. */
    public void lockUntil(Instant until, Instant now) {
        this.lockedUntil = until;
        this.failedAttempts = 0;
        this.updatedAt = now;
    }

    /** Una verificación exitosa reinicia el contador de fallos. */
    public void reset(Instant now) {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getScope() { return scope; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
}
