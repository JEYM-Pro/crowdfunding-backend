package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionItem(
        UUID id,
        BigDecimal amount,
        String type,
        Instant date,
        String campaignName,
        UUID campaignId
) {
}
