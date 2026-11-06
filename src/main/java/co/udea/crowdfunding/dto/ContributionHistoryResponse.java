package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContributionHistoryResponse(
        UUID transactionId,
        UUID campaignId,
        String campaignName,
        BigDecimal amount,
        Instant date,
        UUID rewardId,
        String rewardName,
        String status) {
}
