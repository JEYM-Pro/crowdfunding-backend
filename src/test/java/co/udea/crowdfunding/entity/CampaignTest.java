package co.udea.crowdfunding.entity;

import co.udea.crowdfunding.exception.CampaignNotEditableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void publish_createsActiveCampaignWithoutRaisedFunds() {
        Campaign campaign = newCampaign();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaign.isActive()).isTrue();
        assertThat(campaign.getRaised()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(campaign.getUserId()).isEqualTo(OWNER_ID);
        assertThat(campaign.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(campaign.getCreatedAt()).isEqualTo(NOW);
        assertThat(campaign.getUpdatedAt()).isEqualTo(NOW);
        assertThat(campaign.getImages()).containsExactly("https://img.test/1.png");
    }

    @Test
    void publish_withNullImages_usesEmptyList() {
        Campaign campaign = Campaign.publish(OWNER_ID, "T", "D", BigDecimal.TEN, NOW.plus(1, ChronoUnit.DAYS),
                CATEGORY_ID, null, null, NOW);

        assertThat(campaign.getImages()).isEmpty();
    }

    @Test
    void isOwnedBy_onlyMatchesTheCreator() {
        Campaign campaign = newCampaign();

        assertThat(campaign.isOwnedBy(OWNER_ID)).isTrue();
        assertThat(campaign.isOwnedBy(UUID.randomUUID())).isFalse();
        assertThat(campaign.isOwnedBy(null)).isFalse();
    }

    @Test
    void hasContributions_isTrueOnlyWhenSomethingWasRaised() {
        Campaign campaign = newCampaign();
        assertThat(campaign.hasContributions()).isFalse();

        ReflectionTestUtils.setField(campaign, "raised", new BigDecimal("0.01"));
        assertThat(campaign.hasContributions()).isTrue();
    }

    @Test
    void assertGoalAndDeadlineEditable_passesWithoutContributions() {
        Campaign campaign = newCampaign();

        campaign.assertGoalAndDeadlineEditable();
    }

    @Test
    void changeGoalAndDeadline_withoutContributions_updatesValues() {
        Campaign campaign = newCampaign();
        Instant later = NOW.plus(50, ChronoUnit.DAYS);
        Instant touched = NOW.plusSeconds(60);

        campaign.changeGoal(new BigDecimal("999"), touched);
        campaign.changeDeadline(later, touched);

        assertThat(campaign.getGoal()).isEqualByComparingTo("999");
        assertThat(campaign.getDeadline()).isEqualTo(later);
        assertThat(campaign.getUpdatedAt()).isEqualTo(touched);
    }

    @Test
    void changeGoal_withContributions_isRejected() {
        Campaign campaign = newCampaignWithContributions();

        assertThatThrownBy(() -> campaign.changeGoal(new BigDecimal("999"), NOW))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage(Campaign.LOCKED_MESSAGE);
        assertThat(campaign.getGoal()).isEqualByComparingTo("1000000");
    }

    @Test
    void changeDeadline_withContributions_isRejected() {
        Campaign campaign = newCampaignWithContributions();
        Instant originalDeadline = campaign.getDeadline();

        assertThatThrownBy(() -> campaign.changeDeadline(NOW.plus(80, ChronoUnit.DAYS), NOW))
                .isInstanceOf(CampaignNotEditableException.class);
        assertThat(campaign.getDeadline()).isEqualTo(originalDeadline);
    }

    @Test
    void updateDetails_withContributions_stillAllowedForTitleDescriptionAndImages() {
        Campaign campaign = newCampaignWithContributions();

        campaign.updateDetails("Nuevo", "Nueva desc", List.of("https://img.test/2.png"), NOW.plusSeconds(5));

        assertThat(campaign.getTitle()).isEqualTo("Nuevo");
        assertThat(campaign.getDescription()).isEqualTo("Nueva desc");
        assertThat(campaign.getImages()).containsExactly("https://img.test/2.png");
    }

    @Test
    void updateDetails_withNullImages_keepsCurrentImages() {
        Campaign campaign = newCampaign();

        campaign.updateDetails("Nuevo", "Nueva desc", null, NOW);

        assertThat(campaign.getImages()).containsExactly("https://img.test/1.png");
    }

    @Test
    void publish_keepsRewardsAndNullRewardsMeanNone() {
        assertThat(newCampaign().getRewards()).hasSize(1);
        assertThat(newCampaign().getRewards().get(0).getName()).isEqualTo("Taza");
        assertThat(newCampaign().getRewards().get(0).getStock()).isEqualTo(12);

        Campaign withoutRewards = Campaign.publish(OWNER_ID, "T", "D", BigDecimal.TEN,
                NOW.plus(1, ChronoUnit.DAYS), CATEGORY_ID, null, null, NOW);
        assertThat(withoutRewards.getRewards()).isEmpty();
    }

    @Test
    void getRewards_isNotModifiableFromOutside() {
        List<Reward> rewards = newCampaign().getRewards();

        assertThatThrownBy(() -> rewards.add(new Reward("x", null, BigDecimal.ONE, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void changeCategory_updatesCategoryAndTimestamp() {
        Campaign campaign = newCampaign();
        UUID other = UUID.randomUUID();

        campaign.changeCategory(other, NOW.plusSeconds(30));

        assertThat(campaign.getCategoryId()).isEqualTo(other);
        assertThat(campaign.getUpdatedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void getImages_isNotModifiableFromOutside() {
        Campaign campaign = newCampaign();
        List<String> images = campaign.getImages();

        assertThatThrownBy(() -> images.add("https://img.test/hack.png"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void statusIsNotActive_whenCampaignIsPaused() {
        Campaign campaign = newCampaign();
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.PAUSED);

        assertThat(campaign.isActive()).isFalse();
    }

    private Campaign newCampaign() {
        return Campaign.publish(OWNER_ID, "Título", "Descripción", new BigDecimal("1000000"),
                NOW.plus(30, ChronoUnit.DAYS), CATEGORY_ID, new ArrayList<>(List.of("https://img.test/1.png")),
                List.of(new Reward("Taza", "Pieza única", new BigDecimal("50000"), 12)), NOW);
    }

    private Campaign newCampaignWithContributions() {
        Campaign campaign = newCampaign();
        ReflectionTestUtils.setField(campaign, "raised", new BigDecimal("25000"));
        return campaign;
    }
}
