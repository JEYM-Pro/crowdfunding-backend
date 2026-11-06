package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CampaignEarningsResponse(
        UUID campaignId,
        String campaignName,
        BigDecimal totalRaised,
        long contributorsCount,
        List<CampaignContributionResponse> contributions) {
}
