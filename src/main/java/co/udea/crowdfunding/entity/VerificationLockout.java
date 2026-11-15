package co.udea.crowdfunding.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** HU-S07: registro inalterable de cada bloqueo, con fecha, usuario y campaña involucrada (si la hay). */
@Entity
@Table(name = "verification_lockouts")
public class VerificationLockout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(nullable = false, length = 60)
    private String scope;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "locked_until", nullable = false)
    private Instant lockedUntil;

    protected VerificationLockout() {
    }

    public VerificationLockout(UUID userId, UUID campaignId, String scope, int failedAttempts,
                               Instant lockedAt, Instant lockedUntil) {
        this.userId = userId;
        this.campaignId = campaignId;
        this.scope = scope;
        this.failedAttempts = failedAttempts;
        this.lockedAt = lockedAt;
        this.lockedUntil = lockedUntil;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCampaignId() { return campaignId; }
    public String getScope() { return scope; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedAt() { return lockedAt; }
    public Instant getLockedUntil() { return lockedUntil; }
}
