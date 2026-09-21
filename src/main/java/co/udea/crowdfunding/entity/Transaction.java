package co.udea.crowdfunding.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant date;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "campaign_name")
    private String campaignName;

    protected Transaction() {
    }

    public Transaction(BigDecimal amount, Instant date, String type, UUID userId, UUID campaignId, String campaignName) {
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.userId = userId;
        this.campaignId = campaignId;
        this.campaignName = campaignName;
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public Instant getDate() { return date; }
    public String getType() { return type; }
    public UUID getUserId() { return userId; }
    public UUID getCampaignId() { return campaignId; }
    public String getCampaignName() { return campaignName; }
}
