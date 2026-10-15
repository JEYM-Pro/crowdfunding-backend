package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** HU-12: comprobante de un aporte confirmado. {@code rewardId} y {@code rewardName} son null si no se eligió recompensa. */
public record ContributionConfirmationResponse(
        UUID transactionId,
        UUID campaignId,
        String campaignName,
        BigDecimal amount,
        UUID rewardId,
        String rewardName,
        BigDecimal remainingBalance,
        Instant transactionDate,
        String message
) {
}
