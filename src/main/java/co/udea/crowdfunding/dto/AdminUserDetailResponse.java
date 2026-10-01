package co.udea.crowdfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID id,
        String name,
        String email,
        String role,
        String status,
        BigDecimal balance,
        BigDecimal historic,
        Instant createdAt,
        Instant updatedAt,
        long campaignsCount,
        long transactionsCount
) {
}