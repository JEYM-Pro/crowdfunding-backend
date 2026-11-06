package co.udea.crowdfunding.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID userId, UUID campaignId,
                        String title, String message, Instant createdAt) {
        this.userId = userId;
        this.campaignId = campaignId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.read = false;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCampaignId() { return campaignId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }

    public void markAsRead() {
        this.read = true;
    }
}
