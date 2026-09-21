package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RechargeResponse(
        UUID id,
        BigDecimal newBalance,
        String message
) {
}
