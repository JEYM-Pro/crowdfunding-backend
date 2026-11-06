package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.List;

public record SponsorContributionsSummaryResponse(
        BigDecimal totalContributed,
        List<SponsorContributionResponse> campaigns) {
}
