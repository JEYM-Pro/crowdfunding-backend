
package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ContributionRequest {
    private BigDecimal amount;

    /** HU-11: recompensa elegida (opcional). */
    private UUID rewardId;

    /** HU-S06: contraseña de la cuenta para confirmar el aporte. */
    private String password;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public UUID getRewardId() { return rewardId; }
    public void setRewardId(UUID rewardId) { this.rewardId = rewardId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    /** La contraseña no debe aparecer en registros ni en trazas. */
    @Override
    public String toString() {
        return "ContributionRequest[amount=" + amount + ", rewardId=" + rewardId + ", password=****]";
    }
}
