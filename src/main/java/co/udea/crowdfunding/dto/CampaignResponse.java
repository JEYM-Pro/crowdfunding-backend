package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String title,
        String description,
        BigDecimal goal,
        BigDecimal raised,
        Instant deadline,
        CampaignStatus status,
        UUID categoryId,
        String categoryName,
        List<String> images,
        UUID creatorId,
        boolean hasContributions,
        Instant createdAt,
        Instant updatedAt,
        List<RewardResponse> rewards
) {

    public static CampaignResponse from(Campaign campaign, String categoryName) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getGoal(),
                campaign.getRaised(),
                campaign.getDeadline(),
                campaign.getStatus(),
                campaign.getCategoryId(),
                categoryName,
                List.copyOf(campaign.getImages()),
                campaign.getUserId(),
                campaign.hasContributions(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt(),
                campaign.getRewards().stream().map(RewardResponse::from).toList());
    }
}
