
package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ContributionRequest {
    private BigDecimal amount;

    /** HU-11: recompensa elegida (opcional). */
    private UUID rewardId;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public UUID getRewardId() { return rewardId; }
    public void setRewardId(UUID rewardId) { this.rewardId = rewardId; }
}
