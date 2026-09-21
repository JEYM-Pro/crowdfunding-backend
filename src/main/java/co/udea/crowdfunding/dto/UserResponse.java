package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        BigDecimal balance,
        BigDecimal historic
) {
}
