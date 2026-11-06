package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CampaignContributionResponse(
        UUID transactionId,
        UUID contributorId,
        String contributorName,
        String contributorEmail,
        BigDecimal amount,
        Instant date,
        UUID rewardId,
        String rewardName) {
}
