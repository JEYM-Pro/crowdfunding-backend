package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.User;

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
        String creatorName,
        String creatorEmail,
        boolean hasContributions,
        List<String> backerEmails,
        int contributionsCount,
        Instant createdAt,
        Instant updatedAt,
        List<RewardResponse> rewards
) {

    public static CampaignResponse from(Campaign campaign, String categoryName, User creator) {
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
                creator.getName(),
                creator.getEmail(),
                campaign.hasContributions(),
                List.of(),
                0,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt(),
                campaign.getRewards().stream().map(RewardResponse::from).toList());
    }

    public static CampaignResponse fromWithDetails(Campaign campaign, String categoryName, User creator,
                                                   List<String> backerEmails, int contributionsCount) {
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
                creator.getName(),
                creator.getEmail(),
                campaign.hasContributions(),
                backerEmails,
                contributionsCount,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt(),
                campaign.getRewards().stream().map(RewardResponse::from).toList());
    }
}
