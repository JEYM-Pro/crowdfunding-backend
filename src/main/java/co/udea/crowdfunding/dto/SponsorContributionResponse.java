package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SponsorContributionResponse(
        UUID campaignId,
        String campaignName,
        BigDecimal totalContributed) {
}
