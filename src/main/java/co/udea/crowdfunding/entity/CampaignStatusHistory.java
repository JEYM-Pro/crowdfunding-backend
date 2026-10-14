package co.udea.crowdfunding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Trazabilidad de cada cambio de estado de una campaña (ver modelo de dominio del Sprint 1). */
@Entity
@Table(name = "campaign_status_history")
public class CampaignStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private CampaignStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private CampaignStatus toStatus;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected CampaignStatusHistory() {
    }

    public CampaignStatusHistory(UUID campaignId,
                                 CampaignStatus fromStatus,
                                 CampaignStatus toStatus,
                                 UUID changedByUserId,
                                 Instant changedAt) {
        this.campaignId = campaignId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.changedAt = changedAt;
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public CampaignStatus getFromStatus() { return fromStatus; }
    public CampaignStatus getToStatus() { return toStatus; }
    public UUID getChangedByUserId() { return changedByUserId; }
    public Instant getChangedAt() { return changedAt; }
}
