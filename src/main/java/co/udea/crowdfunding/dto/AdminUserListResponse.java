package co.udea.crowdfunding.dto;

import org.springframework.data.domain.Page;

public record AdminUserListResponse(
        Page<AdminUserSummary> users
) {
}