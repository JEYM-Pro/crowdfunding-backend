package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ContributionConfirmationResponse;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.Reward;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.CampaignNotEditableException;
import co.udea.crowdfunding.exception.CampaignNotFoundException;
import co.udea.crowdfunding.exception.ContributionProcessingException;
import co.udea.crowdfunding.exception.InvalidContributionException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.CampaignStatusHistoryRepository;
import co.udea.crowdfunding.repository.CategoryRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU-11 (nivel de recompensa) y HU-12 (confirmación de contribución). */
@ExtendWith(MockitoExtension.class)
class CampaignContributionConfirmationTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final UUID CREATOR_ID = UUID.randomUUID();
    private static final UUID SPONSOR_ID = UUID.randomUUID();
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID REWARD_ID = UUID.randomUUID();
    private static final UUID TRANSACTION_ID = UUID.randomUUID();

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CampaignStatusHistoryRepository statusHistoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private ContributionEmailService contributionEmailService;

    private CampaignService service;
    private Campaign campaign;
    private Reward reward;
    private User sponsor;

    @BeforeEach
    void setUp() {
        service = new CampaignService(campaignRepository, categoryRepository, statusHistoryRepository,
                userRepository, transactionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        ReflectionTestUtils.setField(service, "contributionEmailService", contributionEmailService);

        reward = new Reward("Camiseta oficial", "Camiseta del proyecto", new BigDecimal("50000"), 3);
        ReflectionTestUtils.setField(reward, "id", REWARD_ID);

        campaign = Campaign.publish(CREATOR_ID, "Huerta comunitaria", "Descripción", new BigDecimal("1000000"),
                NOW.plus(30, ChronoUnit.DAYS), CATEGORY_ID, List.of(), List.of(reward), NOW.minus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);

        sponsor = new User("Beto Patrocinador", "beto@test.com", "hash");
        ReflectionTestUtils.setField(sponsor, "id", SPONSOR_ID);
        sponsor.setBalance(new BigDecimal("100000"));
    }

    // ---------------------------------------------------------------- éxito

    @Test
    void contributionWithReward_updatesEverythingAndReturnsTheReceipt() {
        stubLockedLookups();
        stubSaveTransaction();

        ContributionConfirmationResponse receipt =
                service.makeContributionWithConfirmation(CAMPAIGN_ID, new BigDecimal("60000"), REWARD_ID, SPONSOR_ID);

        assertThat(sponsor.getBalance()).isEqualByComparingTo("40000");
        assertThat(sponsor.getHistoric()).isEqualByComparingTo("60000");
        assertThat(campaign.getRaised()).isEqualByComparingTo("60000");
        assertThat(reward.getStock()).isEqualTo(2);

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo("contribution");
        assertThat(saved.getValue().getUserId()).isEqualTo(SPONSOR_ID);
        assertThat(saved.getValue().getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(saved.getValue().getCampaignName()).isEqualTo("Huerta comunitaria");
        assertThat(saved.getValue().getRewardId()).isEqualTo(REWARD_ID);
        assertThat(saved.getValue().getRewardName()).isEqualTo("Camiseta oficial");

        assertThat(receipt.transactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(receipt.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(receipt.campaignName()).isEqualTo("Huerta comunitaria");
        assertThat(receipt.amount()).isEqualByComparingTo("60000");
        assertThat(receipt.rewardId()).isEqualTo(REWARD_ID);
        assertThat(receipt.rewardName()).isEqualTo("Camiseta oficial");
        assertThat(receipt.remainingBalance()).isEqualByComparingTo("40000");
        assertThat(receipt.transactionDate()).isEqualTo(NOW);
        assertThat(receipt.message()).isEqualTo(CampaignService.CONTRIBUTION_SUCCESS_MESSAGE);

        verify(contributionEmailService).sendConfirmation("beto@test.com", receipt);
    }

    @Test
    void contributionWithoutReward_succeedsAndLeavesStockUntouched() {
        stubLockedLookups();
        stubSaveTransaction();

        ContributionConfirmationResponse receipt =
                service.makeContributionWithConfirmation(CAMPAIGN_ID, new BigDecimal("25000"), null, SPONSOR_ID);

        assertThat(receipt.rewardId()).isNull();
        assertThat(receipt.rewardName()).isNull();
        assertThat(receipt.remainingBalance()).isEqualByComparingTo("75000");
        assertThat(reward.getStock()).isEqualTo(3);
        verify(contributionEmailService).sendConfirmation("beto@test.com", receipt);
    }

    @Test
    void lastUnitOfAReward_leavesItSoldOut() {
        ReflectionTestUtils.setField(reward, "stock", 1);
        stubLockedLookups();
        stubSaveTransaction();

        service.makeContributionWithConfirmation(CAMPAIGN_ID, new BigDecimal("50000"), REWARD_ID, SPONSOR_ID);

        assertThat(reward.getStock()).isZero();
    }

    // ------------------------------------------------------------- rechazos

    @Test
    void rewardBelowItsMinimumAmount_isRejectedWithoutRecordingAnything() {
        stubLockedLookups();

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("30000"), REWARD_ID, SPONSOR_ID))
                .isInstanceOf(InvalidContributionException.class)
                .hasMessageContaining("50000");

        assertNothingChanged();
    }

    @Test
    void soldOutReward_isRejected() {
        ReflectionTestUtils.setField(reward, "stock", 0);
        stubLockedLookups();

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), REWARD_ID, SPONSOR_ID))
                .isInstanceOf(InvalidContributionException.class)
                .hasMessage("La recompensa seleccionada está agotada.");

        assertNothingChanged();
    }

    @Test
    void rewardFromAnotherCampaign_isRejected() {
        stubLockedLookups();

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), UUID.randomUUID(), SPONSOR_ID))
                .isInstanceOf(InvalidContributionException.class)
                .hasMessage("La recompensa seleccionada no pertenece a esta campaña.");

        assertNothingChanged();
    }

    @Test
    void insufficientBalance_isRejected() {
        sponsor.setBalance(new BigDecimal("10000"));
        stubLockedLookups();

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), null, SPONSOR_ID))
                .isInstanceOf(InvalidContributionException.class)
                .hasMessage("Saldo insuficiente para realizar el aporte.");

        assertThat(sponsor.getBalance()).isEqualByComparingTo("10000");
        assertThat(sponsor.getHistoric()).isEqualByComparingTo("0");
        assertThat(campaign.getRaised()).isEqualByComparingTo("0");
        verify(transactionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(contributionEmailService);
    }

    @Test
    void zeroNegativeOrMissingAmount_isRejectedBeforeTouchingTheDatabase() {
        for (BigDecimal amount : new BigDecimal[]{BigDecimal.ZERO, new BigDecimal("-5"), null}) {
            assertThatThrownBy(() -> service.makeContributionWithConfirmation(CAMPAIGN_ID, amount, null, SPONSOR_ID))
                    .isInstanceOf(InvalidContributionException.class)
                    .hasMessage("El monto a aportar debe ser mayor a cero.");
        }
        verifyNoInteractions(campaignRepository, userRepository, transactionRepository, contributionEmailService);
    }

    @Test
    void inactiveCampaign_isRejected() {
        campaign.setStatus(CampaignStatus.CLOSED);
        when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), null, SPONSOR_ID))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage(CampaignService.CONTRIBUTION_NOT_ACTIVE_MESSAGE);

        verifyNoInteractions(userRepository, transactionRepository, contributionEmailService);
    }

    @Test
    void unknownCampaign_isNotFound() {
        when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), null, SPONSOR_ID))
                .isInstanceOf(CampaignNotFoundException.class);
    }

    @Test
    void unknownSponsor_isRejected() {
        when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(userRepository.findByIdForUpdate(SPONSOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), null, SPONSOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario no encontrado.");
    }

    // ---------------------------------------------------------------- correo

    @Test
    void emailFailure_propagatesSoTheTransactionIsRolledBack() {
        stubLockedLookups();
        stubSaveTransaction();
        doThrow(new ContributionProcessingException("SMTP caído"))
                .when(contributionEmailService).sendConfirmation(any(), any());

        assertThatThrownBy(() -> service.makeContributionWithConfirmation(
                CAMPAIGN_ID, new BigDecimal("60000"), REWARD_ID, SPONSOR_ID))
                .isInstanceOf(ContributionProcessingException.class)
                .hasMessage("SMTP caído");
    }

    // --------------------------------------------------------------- helpers

    private void stubLockedLookups() {
        when(campaignRepository.findByIdForUpdate(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(userRepository.findByIdForUpdate(SPONSOR_ID)).thenReturn(Optional.of(sponsor));
    }

    private void stubSaveTransaction() {
        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", TRANSACTION_ID);
            return transaction;
        });
    }

    private void assertNothingChanged() {
        assertThat(sponsor.getBalance()).isEqualByComparingTo("100000");
        assertThat(sponsor.getHistoric()).isEqualByComparingTo("0");
        assertThat(campaign.getRaised()).isEqualByComparingTo("0");
        verify(transactionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(contributionEmailService);
    }
}
