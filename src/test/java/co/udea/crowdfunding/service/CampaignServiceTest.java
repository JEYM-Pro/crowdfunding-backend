package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.CampaignResponse;
import co.udea.crowdfunding.dto.CreateCampaignRequest;
import co.udea.crowdfunding.dto.RewardRequest;
import co.udea.crowdfunding.dto.UpdateCampaignRequest;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.CampaignStatusHistory;
import co.udea.crowdfunding.entity.Category;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.CampaignAccessDeniedException;
import co.udea.crowdfunding.exception.CampaignNotEditableException;
import co.udea.crowdfunding.exception.CampaignNotFoundException;
import co.udea.crowdfunding.exception.InvalidCampaignException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.CampaignStatusHistoryRepository;
import co.udea.crowdfunding.repository.CategoryRepository;
import co.udea.crowdfunding.repository.UserRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final BigDecimal GOAL = new BigDecimal("1000000");

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

    private CampaignService service;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new CampaignService(campaignRepository, categoryRepository, statusHistoryRepository,
                userRepository, transactionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        category = new Category("Tecnología");
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        stubUser("creator");
    }

    // =====================================================================
    // HU-06: publicar campaña
    // =====================================================================

    @Test
    void create_withValidData_publishesActiveCampaignAndRecordsHistory() {
        stubUser("sponsor");
        stubCategory();
        stubSaveCampaign();

        CampaignResponse response = service.create(USER_ID,
                createRequest(NOW.plus(30, ChronoUnit.DAYS), List.of(" https://img.test/1.png ")));

        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(response.title()).isEqualTo("Mi campaña");
        assertThat(response.description()).isEqualTo("Descripción del proyecto");
        assertThat(response.goal()).isEqualByComparingTo("5000000");
        assertThat(response.raised()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.categoryName()).isEqualTo("Tecnología");
        assertThat(response.images()).containsExactly("https://img.test/1.png");
        assertThat(response.creatorId()).isEqualTo(USER_ID);
        assertThat(response.hasContributions()).isFalse();

        ArgumentCaptor<CampaignStatusHistory> history = ArgumentCaptor.forClass(CampaignStatusHistory.class);
        verify(statusHistoryRepository).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isNull();
        assertThat(history.getValue().getToStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(history.getValue().getChangedByUserId()).isEqualTo(USER_ID);
        assertThat(history.getValue().getChangedAt()).isEqualTo(NOW);
    }

    @Test
    void create_bySponsor_promotesUserToCreator() {
        User sponsor = stubUser("sponsor");
        stubCategory();
        stubSaveCampaign();

        service.create(USER_ID, createRequest(NOW.plus(10, ChronoUnit.DAYS), null));

        assertThat(sponsor.getRole()).isEqualTo("creator");
        assertThat(sponsor.getUpdatedAt()).isEqualTo(NOW);
        verify(userRepository).save(sponsor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"creator", "admin"})
    void create_byCreatorOrAdmin_keepsRole(String role) {
        User user = stubUser(role);
        stubCategory();
        stubSaveCampaign();

        service.create(USER_ID, createRequest(NOW.plus(10, ChronoUnit.DAYS), null));

        assertThat(user.getRole()).isEqualTo(role);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void create_withoutImages_createsCampaignWithEmptyImageList() {
        stubUser("creator");
        stubCategory();
        stubSaveCampaign();

        CampaignResponse response = service.create(USER_ID, createRequest(NOW.plus(10, ChronoUnit.DAYS), null));

        assertThat(response.images()).isEmpty();
    }

    @Test
    void create_withRewards_savesThemWithTheCampaign() {
        stubUser("creator");
        stubCategory();
        stubSaveCampaign();
        CreateCampaignRequest request = new CreateCampaignRequest("Mi campaña", "Descripción",
                new BigDecimal("5000000"), NOW.plus(30, ChronoUnit.DAYS), CATEGORY_ID, null,
                List.of(new RewardRequest(" Taza artesanal ", " Pieza única ", new BigDecimal("50000"), 12),
                        new RewardRequest("Clase privada", null, new BigDecimal("120000"), 4)));

        CampaignResponse response = service.create(USER_ID, request);

        assertThat(response.rewards()).hasSize(2);
        assertThat(response.rewards().get(0).name()).isEqualTo("Taza artesanal");
        assertThat(response.rewards().get(0).description()).isEqualTo("Pieza única");
        assertThat(response.rewards().get(0).minAmount()).isEqualByComparingTo("50000");
        assertThat(response.rewards().get(0).stock()).isEqualTo(12);
        assertThat(response.rewards().get(1).description()).isNull();
    }

    @Test
    void create_withoutRewards_createsCampaignWithEmptyRewardList() {
        stubUser("creator");
        stubCategory();
        stubSaveCampaign();

        CampaignResponse response = service.create(USER_ID, createRequest(NOW.plus(10, ChronoUnit.DAYS), null));

        assertThat(response.rewards()).isEmpty();
    }

    @Test
    void create_withDeadlineExactlyNow_isRejected() {
        stubUser("sponsor");

        assertThatThrownBy(() -> service.create(USER_ID, createRequest(NOW, null)))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage(CampaignService.DEADLINE_NOT_FUTURE_MESSAGE);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void create_withDeadlineInThePast_isRejected() {
        stubUser("sponsor");

        assertThatThrownBy(() -> service.create(USER_ID, createRequest(NOW.minus(1, ChronoUnit.DAYS), null)))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage(CampaignService.DEADLINE_NOT_FUTURE_MESSAGE);
    }

    @Test
    void create_withDeadlineBeyondMaxDuration_isRejected() {
        stubUser("sponsor");
        Instant tooFar = NOW.plus(90, ChronoUnit.DAYS).plusSeconds(1);

        assertThatThrownBy(() -> service.create(USER_ID, createRequest(tooFar, null)))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage(CampaignService.DEADLINE_TOO_FAR_MESSAGE);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void create_withDeadlineExactlyAtMaxDuration_isAccepted() {
        stubUser("sponsor");
        stubCategory();
        stubSaveCampaign();

        CampaignResponse response = service.create(USER_ID, createRequest(NOW.plus(90, ChronoUnit.DAYS), null));

        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void create_withUnknownCategory_isRejected() {
        stubUser("sponsor");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, createRequest(NOW.plus(10, ChronoUnit.DAYS), null)))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage(CampaignService.CATEGORY_NOT_FOUND_MESSAGE);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void create_withUnknownUser_throwsNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID, createRequest(NOW.plus(10, ChronoUnit.DAYS), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrado");
    }

    // =====================================================================
    // HU-07: editar campaña activa
    // =====================================================================

    @Test
    void update_byOwnerOnActiveCampaign_updatesTitleDescriptionAndImages() {
        Campaign campaign = stubCampaign(false);
        stubSaveAndCategory();

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID, updateRequest(
                "  Nuevo título ", " Nueva descripción ", List.of("https://img.test/a.png", "https://img.test/b.png"),
                null, null));

        assertThat(response.title()).isEqualTo("Nuevo título");
        assertThat(response.description()).isEqualTo("Nueva descripción");
        assertThat(response.images()).containsExactly("https://img.test/a.png", "https://img.test/b.png");
        assertThat(response.goal()).isEqualByComparingTo(GOAL);
        assertThat(campaign.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void update_withNullImages_keepsExistingImages() {
        stubCampaign(false);
        stubSaveAndCategory();

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, null));

        assertThat(response.images()).containsExactly("https://img.test/old.png");
    }

    @Test
    void update_withEmptyImageList_removesImages() {
        stubCampaign(false);
        stubSaveAndCategory();

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", List.of(), null, null));

        assertThat(response.images()).isEmpty();
    }

    @Test
    void update_withRewards_isRejectedBecauseRewardsAreLocked() {
        stubCampaign(false);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID, new UpdateCampaignRequest(
                "Título", "Descripción", null, null, null, null,
                List.of(new RewardRequest("Nueva", null, BigDecimal.TEN, 1)))))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage("La lista de recompensas no se puede modificar una vez publicada la campaña.");
        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void update_withEmptyRewardList_isIgnored() {
        stubCampaign(false);
        stubSaveAndCategory();

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID, new UpdateCampaignRequest(
                "Título", "Descripción", null, null, null, null, List.of()));

        assertThat(response.rewards()).isEmpty();
    }

    @Test
    void update_withNewCategory_changesTheCategory() {
        UUID otherCategoryId = UUID.randomUUID();
        Category other = new Category("Salud");
        ReflectionTestUtils.setField(other, "id", otherCategoryId);
        stubCampaign(false);
        stubSaveCampaign();
        when(categoryRepository.findById(otherCategoryId)).thenReturn(Optional.of(other));

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID, new UpdateCampaignRequest(
                "Título", "Descripción", null, null, null, otherCategoryId, null));

        assertThat(response.categoryId()).isEqualTo(otherCategoryId);
        assertThat(response.categoryName()).isEqualTo("Salud");
    }

    @Test
    void update_withUnknownCategory_isRejected() {
        UUID unknown = UUID.randomUUID();
        stubCampaign(false);
        when(categoryRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID, new UpdateCampaignRequest(
                "Título", "Descripción", null, null, null, unknown, null)))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage("La categoría seleccionada no existe.");
    }

    @Test
    void update_ofUnknownCampaign_throwsNotFound() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, null)))
                .isInstanceOf(CampaignNotFoundException.class);
    }

    @Test
    void update_byNonOwner_isDenied() {
        stubCampaign(false);

        assertThatThrownBy(() -> service.update(OTHER_USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, null)))
                .isInstanceOf(CampaignAccessDeniedException.class)
                .hasMessage("No tienes permiso para editar esta campaña.");

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @ParameterizedTest
    @EnumSource(value = CampaignStatus.class, names = {"PAUSED", "CLOSED", "FINISHED"})
    void update_ofNonActiveCampaign_isRejected(CampaignStatus status) {
        Campaign campaign = stubCampaign(false);
        ReflectionTestUtils.setField(campaign, "status", status);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, null)))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage(CampaignService.NOT_ACTIVE_MESSAGE);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void update_changingGoalWhenCampaignHasContributions_isRejected() {
        stubCampaign(true);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, new BigDecimal("2000000"), null)))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage(Campaign.LOCKED_MESSAGE);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void update_changingDeadlineWhenCampaignHasContributions_isRejected() {
        stubCampaign(true);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, NOW.plus(40, ChronoUnit.DAYS))))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage(Campaign.LOCKED_MESSAGE);
    }

    @Test
    void update_withContributionsAndInvalidDeadline_reportsLockedRuleFirst() {
        stubCampaign(true);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, NOW.minus(1, ChronoUnit.DAYS))))
                .isInstanceOf(CampaignNotEditableException.class)
                .hasMessage(Campaign.LOCKED_MESSAGE);
    }

    @Test
    void update_withContributionsButSameGoalAndDeadline_onlyUpdatesDetails() {
        Campaign campaign = stubCampaign(true);
        stubSaveAndCategory();

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID, updateRequest(
                "Título nuevo", "Descripción nueva", null,
                new BigDecimal("1000000.00"), campaign.getDeadline()));

        assertThat(response.title()).isEqualTo("Título nuevo");
        assertThat(response.goal()).isEqualByComparingTo(GOAL);
        assertThat(response.deadline()).isEqualTo(campaign.getDeadline());
        assertThat(response.hasContributions()).isTrue();
    }

    @Test
    void update_withoutContributions_canChangeGoalAndDeadline() {
        stubCampaign(false);
        stubSaveAndCategory();
        Instant newDeadline = NOW.plus(60, ChronoUnit.DAYS);

        CampaignResponse response = service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, new BigDecimal("2500000"), newDeadline));

        assertThat(response.goal()).isEqualByComparingTo("2500000");
        assertThat(response.deadline()).isEqualTo(newDeadline);
    }

    @Test
    void update_withoutContributions_rejectsDeadlineInThePast() {
        stubCampaign(false);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, NOW.minus(1, ChronoUnit.DAYS))))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage(CampaignService.DEADLINE_NOT_FUTURE_MESSAGE);

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void update_withoutContributions_rejectsDeadlineBeyondMaxDuration() {
        stubCampaign(false);

        assertThatThrownBy(() -> service.update(USER_ID, CAMPAIGN_ID,
                updateRequest("Título", "Descripción", null, null, NOW.plus(91, ChronoUnit.DAYS))))
                .isInstanceOf(InvalidCampaignException.class)
                .hasMessage(CampaignService.DEADLINE_TOO_FAR_MESSAGE);
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    @Test
    void getById_returnsCampaignWithCategoryName() {
        stubCampaign(false);
        stubCategory();

        CampaignResponse response = service.getById(CAMPAIGN_ID);

        assertThat(response.title()).isEqualTo("Título original");
        assertThat(response.categoryName()).isEqualTo("Tecnología");
    }

    @Test
    void listManaged_includesPausedCampaignsForTheirCreator() {
        Campaign campaign = newCampaign(false);
        campaign.setStatus(CampaignStatus.PAUSED);
        User creator = stubUser("creator");
        ReflectionTestUtils.setField(creator, "id", USER_ID);
        when(campaignRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(campaign));
        when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
        when(userRepository.findAllById(any())).thenReturn(List.of(creator));

        List<CampaignResponse> campaigns = service.listManaged(USER_ID, false);

        assertThat(campaigns).hasSize(1);
        assertThat(campaigns.getFirst().status()).isEqualTo(CampaignStatus.PAUSED);
    }

    @Test
    void getById_ofUnknownCampaign_throwsNotFound() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(CAMPAIGN_ID)).isInstanceOf(CampaignNotFoundException.class);
    }

    @Test
    void listActive_returnsOnlyActiveCampaignsWithCategoryNames() {
        Campaign first = newCampaign(false);
        Campaign second = newCampaign(false);
        when(campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(categoryRepository.findAllById(List.of(CATEGORY_ID))).thenReturn(List.of(category));
        User creatorUser = new User("Ana Pérez", "ana@correo.com", "hash");
        ReflectionTestUtils.setField(creatorUser, "id", USER_ID);
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(creatorUser));

        List<CampaignResponse> result = service.listActive();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(item -> {
            assertThat(item.status()).isEqualTo(CampaignStatus.ACTIVE);
            assertThat(item.categoryName()).isEqualTo("Tecnología");
        });
    }

    @Test
    void listActive_withoutCampaigns_returnsEmptyList() {
        when(campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE)).thenReturn(List.of());

        assertThat(service.listActive()).isEmpty();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private User stubUser(String role) {
        User user = new User("Ana Pérez", "ana@correo.com", "hash");
        user.setRole(role);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        return user;
    }

    private void stubCategory() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
    }

    private void stubSaveCampaign() {
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubSaveAndCategory() {
        stubSaveCampaign();
        stubCategory();
    }

    private Campaign stubCampaign(boolean withContributions) {
        Campaign campaign = newCampaign(withContributions);
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        return campaign;
    }

    private Campaign newCampaign(boolean withContributions) {
        Campaign campaign = Campaign.publish(USER_ID, "Título original", "Descripción original", GOAL,
                NOW.plus(30, ChronoUnit.DAYS), CATEGORY_ID, List.of("https://img.test/old.png"), List.of(),
                NOW.minus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        if (withContributions) {
            ReflectionTestUtils.setField(campaign, "raised", new BigDecimal("50000.00"));
        }
        return campaign;
    }

    private CreateCampaignRequest createRequest(Instant deadline, List<String> images) {
        return new CreateCampaignRequest("  Mi campaña ", " Descripción del proyecto ",
                new BigDecimal("5000000"), deadline, CATEGORY_ID, images, null);
    }

    private UpdateCampaignRequest updateRequest(String title, String description, List<String> images,
                                                BigDecimal goal, Instant deadline) {
        return new UpdateCampaignRequest(title, description, images, goal, deadline, null, null);
    }
}
