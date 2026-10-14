
package co.udea.crowdfunding.dto;

import java.math.BigDecimal;

public class ContributionRequest {
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
