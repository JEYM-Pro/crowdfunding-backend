
package co.udea.crowdfunding.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "campaign_milestones",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"campaign_id", "percentage"}
    )
)
public class CampaignMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(nullable = false)
    private int percentage;

    @Column(name = "reached_at", nullable = false)
    private Instant reachedAt;

    protected CampaignMilestone() {
    }

    public CampaignMilestone(UUID campaignId, int percentage, Instant reachedAt) {
        this.campaignId = campaignId;
        this.percentage = percentage;
        this.reachedAt = reachedAt;
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public int getPercentage() { return percentage; }
    public Instant getReachedAt() { return reachedAt; }
}
