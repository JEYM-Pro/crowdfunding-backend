package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.CampaignStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** HU-16: un aporte del historial; {@code campaignStatus} es el estado actual de la campaña, no el del aporte. */
public record ContributionHistoryResponse(
        UUID transactionId,
        UUID campaignId,
        String campaignName,
        BigDecimal amount,
        Instant date,
        UUID rewardId,
        String rewardName,
        CampaignStatus campaignStatus) {
}
