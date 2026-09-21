package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LoginResponse(
        String token,
        UUID id,
        String name,
        String email,
        String role,
        BigDecimal balance
) {
}
