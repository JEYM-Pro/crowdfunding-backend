package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.Reward;

import java.math.BigDecimal;
import java.util.UUID;

public record RewardResponse(UUID id, String name, String description, BigDecimal minAmount, int stock) {

    public static RewardResponse from(Reward reward) {
        return new RewardResponse(reward.getId(), reward.getName(), reward.getDescription(),
                reward.getMinAmount(), reward.getStock());
    }
}
