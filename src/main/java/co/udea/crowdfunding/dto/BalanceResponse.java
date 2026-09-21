package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.List;

public record BalanceResponse(
        BigDecimal balance,
        BigDecimal historic,
        List<CampaignSummary> campaigns,
        List<TransactionItem> history
) {
}
