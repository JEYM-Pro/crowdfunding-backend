package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ContributionHistoryResponse;
import co.udea.crowdfunding.dto.ContributionHistorySummaryResponse;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/** HU-16: historial de aportes con el estado actual de cada campaña. */
@ExtendWith(MockitoExtension.class)
class ContributionHistoryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CampaignRepository campaignRepository;

    @Test
    void getHistory_reportsTheCurrentStatusOfEachCampaignAndTheTotal() {
        Campaign active = campaign("Huerta comunitaria", CampaignStatus.ACTIVE);
        Campaign finished = campaign("Biblioteca rural", CampaignStatus.FINISHED);
        when(transactionRepository.findByUserIdAndTypeOrderByDateDesc(USER_ID, "contribution")).thenReturn(List.of(
                contribution(active, "30000"),
                contribution(finished, "20000"),
                contribution(active, "10000")));
        when(campaignRepository.findAllById(anyList())).thenReturn(List.of(active, finished));

        ContributionHistorySummaryResponse history =
                new ContributionHistoryService(transactionRepository, campaignRepository).getHistory(USER_ID);

        assertThat(history.totalContributed()).isEqualByComparingTo("60000");
        assertThat(history.contributionsCount()).isEqualTo(3);
        assertThat(history.contributions())
                .extracting(ContributionHistoryResponse::campaignName, ContributionHistoryResponse::campaignStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Huerta comunitaria", CampaignStatus.ACTIVE),
                        org.assertj.core.groups.Tuple.tuple("Biblioteca rural", CampaignStatus.FINISHED),
                        org.assertj.core.groups.Tuple.tuple("Huerta comunitaria", CampaignStatus.ACTIVE));
    }

    @Test
    void getHistory_withoutContributions_returnsAnEmptyHistory() {
        when(transactionRepository.findByUserIdAndTypeOrderByDateDesc(USER_ID, "contribution")).thenReturn(List.of());
        when(campaignRepository.findAllById(anyList())).thenReturn(List.of());

        ContributionHistorySummaryResponse history =
                new ContributionHistoryService(transactionRepository, campaignRepository).getHistory(USER_ID);

        assertThat(history.totalContributed()).isEqualByComparingTo("0");
        assertThat(history.contributions()).isEmpty();
    }

    private Campaign campaign(String title, CampaignStatus status) {
        Campaign campaign = Campaign.publish(UUID.randomUUID(), title, "Descripción", new BigDecimal("1000000"),
                NOW.plus(30, ChronoUnit.DAYS), UUID.randomUUID(), List.of(), List.of(), "hash", NOW);
        ReflectionTestUtils.setField(campaign, "id", UUID.randomUUID());
        campaign.setStatus(status);
        return campaign;
    }

    private Transaction contribution(Campaign campaign, String amount) {
        return new Transaction(new BigDecimal(amount), NOW, "contribution", USER_ID,
                campaign.getId(), campaign.getTitle());
    }
}
