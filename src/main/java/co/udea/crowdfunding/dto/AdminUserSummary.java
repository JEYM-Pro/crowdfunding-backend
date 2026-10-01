package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminUserSummary(
        UUID id,
        String name,
        String email,
        String role,
        String status,
        Instant createdAt
) {
}