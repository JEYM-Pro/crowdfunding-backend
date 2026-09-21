package co.udea.crowdfunding.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal raised = BigDecimal.ZERO;

    @Column(name = "goal", nullable = false, precision = 12, scale = 2)
    private BigDecimal goal;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected Campaign() {
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public BigDecimal getRaised() { return raised; }
    public BigDecimal getGoal() { return goal; }
    public UUID getUserId() { return userId; }
}
