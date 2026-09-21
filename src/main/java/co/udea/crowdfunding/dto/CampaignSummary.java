package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CampaignSummary(
        UUID id,
        String title,
        BigDecimal raised,
        BigDecimal goal,
        BigDecimal contributed
) {
}
