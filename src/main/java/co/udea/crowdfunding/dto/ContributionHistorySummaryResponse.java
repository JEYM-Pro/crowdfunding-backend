package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.List;

public record ContributionHistorySummaryResponse(
        BigDecimal totalContributed,
        int contributionsCount,
        List<ContributionHistoryResponse> contributions) {
}
