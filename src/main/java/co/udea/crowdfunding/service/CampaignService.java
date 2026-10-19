package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.CampaignResponse;
import co.udea.crowdfunding.dto.ContributionConfirmationResponse;
import co.udea.crowdfunding.dto.CreateCampaignRequest;
import co.udea.crowdfunding.dto.RewardRequest;
import co.udea.crowdfunding.dto.UpdateCampaignRequest;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.CampaignStatusHistory;
import co.udea.crowdfunding.entity.Category;
import co.udea.crowdfunding.entity.Reward;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.CampaignAccessDeniedException;
import co.udea.crowdfunding.exception.CampaignNotEditableException;
import co.udea.crowdfunding.exception.CampaignNotFoundException;
import co.udea.crowdfunding.exception.InvalidCampaignException;
import co.udea.crowdfunding.exception.InvalidContributionException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.CampaignStatusHistoryRepository;
import co.udea.crowdfunding.repository.CategoryRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CampaignService {

    static final int MAX_DURATION_DAYS = 90;
    static final String DEADLINE_NOT_FUTURE_MESSAGE = "La fecha límite debe ser posterior a hoy.";
    static final String DEADLINE_TOO_FAR_MESSAGE = "La fecha límite no puede superar los 90 días.";
    static final String CATEGORY_NOT_FOUND_MESSAGE = "La categoría seleccionada no existe.";
    static final String NOT_ACTIVE_MESSAGE = "Solo se pueden editar campañas activas.";
    static final String REWARDS_LOCKED_MESSAGE = "La lista de recompensas no se puede modificar una vez publicada la campaña.";

    static final String CONTRIBUTION_TYPE = "contribution";
    static final String CONTRIBUTION_NOT_ACTIVE_MESSAGE = "No se pueden realizar aportes a una campaña que no está activa.";
    static final String CONTRIBUTION_SUCCESS_MESSAGE = "Aporte realizado exitosamente.";

    private static final String ROLE_SPONSOR = "sponsor";
    private static final String ROLE_CREATOR = "creator";
    private final CampaignRepository campaignRepository;
    private final CategoryRepository categoryRepository;
    private final CampaignStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public CampaignService(CampaignRepository campaignRepository,
                           CategoryRepository categoryRepository,
                           CampaignStatusHistoryRepository statusHistoryRepository,
                           UserRepository userRepository,
                           TransactionRepository transactionRepository,
                           Clock clock) {
        this.campaignRepository = campaignRepository;
        this.categoryRepository = categoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional
    public CampaignResponse create(UUID userId, CreateCampaignRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Instant now = clock.instant();
        validateDeadline(request.deadline(), now);
        Category category;
        if (request.categoryId() != null) {
            try {
                category = findCategory(request.categoryId());
            } catch (InvalidCampaignException e) {
                category = getDefaultCategory();
            }
        } else {
            category = getDefaultCategory();
        }

        Campaign campaign = campaignRepository.save(Campaign.publish(
                userId,
                request.title().trim(),
                request.description().trim(),
                request.goal(),
                request.deadline(),
                category.getId(),
                cleanImages(request.images()),
                toRewards(request.rewards()),
                now));

        statusHistoryRepository.save(
                new CampaignStatusHistory(campaign.getId(), null, CampaignStatus.ACTIVE, userId, now));

        promoteToCreator(creator, now);

        return CampaignResponse.fromWithDetails(
                campaign, category.getName(), creator, List.of(), 0);
    }

    @Transactional
    public CampaignResponse update(UUID userId, UUID campaignId, UpdateCampaignRequest request) {
        Campaign campaign = findCampaign(campaignId);

        if (!campaign.isOwnedBy(userId)) {
            throw new CampaignAccessDeniedException();
        }
        if (!campaign.isActive()) {
            throw new CampaignNotEditableException(NOT_ACTIVE_MESSAGE);
        }
        if (request.rewards() != null && !request.rewards().isEmpty()) {
            throw new CampaignNotEditableException(REWARDS_LOCKED_MESSAGE);
        }

        Instant now = clock.instant();
        boolean goalChanged = hasChangedGoal(campaign, request.goal());
        boolean deadlineChanged = hasChangedDeadline(campaign, request.deadline());

        if (goalChanged || deadlineChanged) {
            campaign.assertGoalAndDeadlineEditable();
        }
        if (deadlineChanged) {
            validateDeadline(request.deadline(), now);
        }

        campaign.updateDetails(
                request.title().trim(),
                request.description().trim(),
                request.images() == null ? null : cleanImages(request.images()),
                now);
        if (request.categoryId() != null && !request.categoryId().equals(campaign.getCategoryId())) {
            campaign.changeCategory(findCategory(request.categoryId()).getId(), now);
        }
        if (goalChanged) {
            campaign.changeGoal(request.goal(), now);
        }
        if (deadlineChanged) {
            campaign.changeDeadline(request.deadline(), now);
        }

        Campaign saved = campaignRepository.save(campaign);
        User creator = userRepository.findById(saved.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        List<String> backerEmails = transactionRepository.findDistinctUserEmailByCampaignId(saved.getId());
        int contributionsCount = (int) transactionRepository.countByCampaignId(saved.getId());

        return CampaignResponse.fromWithDetails(
                saved, categoryName(saved.getCategoryId()), creator, backerEmails, contributionsCount);
    }

    public CampaignResponse getById(UUID campaignId) {
        Campaign campaign = findCampaign(campaignId);
        User creator = userRepository.findById(campaign.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        List<String> backerEmails = transactionRepository.findDistinctUserEmailByCampaignId(campaignId);
        int contributionsCount = (int) transactionRepository.countByCampaignId(campaignId);
        return CampaignResponse.fromWithDetails(
                campaign, categoryName(campaign.getCategoryId()), creator, backerEmails, contributionsCount);
    }

    public List<CampaignResponse> listActive() {
        List<Campaign> campaigns = campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE);
        return toCampaignResponses(campaigns);
    }

    public List<CampaignResponse> listManaged(UUID userId, boolean admin) {
        List<Campaign> campaigns = admin
                ? campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : campaignRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return toCampaignResponses(campaigns);
    }

    private List<CampaignResponse> toCampaignResponses(List<Campaign> campaigns) {
        Map<UUID, String> categoryNames = categoryRepository
                .findAllById(campaigns.stream().map(Campaign::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (first, second) -> first));

        Map<UUID, User> creators = userRepository
                .findAllById(campaigns.stream().map(Campaign::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u, (first, second) -> first));

        return campaigns.stream()
                .map((campaign) -> {
                    User creator = creators.get(campaign.getUserId());
                    List<String> backerEmails = transactionRepository.findDistinctUserEmailByCampaignId(campaign.getId());
                    int contributionsCount = (int) transactionRepository.countByCampaignId(campaign.getId());
                    return CampaignResponse.fromWithDetails(
                            campaign, categoryNames.get(campaign.getCategoryId()), creator, backerEmails, contributionsCount);
                })
                .toList();
    }

    public List<CampaignResponse> exploreActiveCampaigns(String nameFilter, UUID categoryIdFilter, String sortBy) {
        Sort sortCriteria = Sort.by(Sort.Direction.DESC, "createdAt");

        if (sortBy != null && !sortBy.isBlank()) {
            switch (sortBy.toLowerCase()) {
                case "recaudo":
                    sortCriteria = Sort.by(Sort.Direction.DESC, "raised");
                    break;
                case "fecha":
                    sortCriteria = Sort.by(Sort.Direction.ASC, "deadline");
                    break;
                case "popularidad":
                    sortCriteria = Sort.by(Sort.Direction.DESC, "raised");
                    break;
            }
        }

        List<Campaign> campaigns;
        boolean hasName = (nameFilter != null && !nameFilter.isBlank());
        boolean hasCategory = (categoryIdFilter != null);

        if (hasName && hasCategory) {
            campaigns = campaignRepository.findByTitleContainingIgnoreCaseAndCategoryIdAndStatus(nameFilter, categoryIdFilter, CampaignStatus.ACTIVE, sortCriteria);
        } else if (hasName) {
            campaigns = campaignRepository.findByTitleContainingIgnoreCaseAndStatus(nameFilter, CampaignStatus.ACTIVE, sortCriteria);
        } else if (hasCategory) {
            campaigns = campaignRepository.findByCategoryIdAndStatus(categoryIdFilter, CampaignStatus.ACTIVE, sortCriteria);
        } else {
            campaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE, sortCriteria);
        }

        Map<UUID, String> categoryNames = categoryRepository
                .findAllById(campaigns.stream().map(Campaign::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (first, second) -> first));

        Map<UUID, User> creators = userRepository
                .findAllById(campaigns.stream().map(Campaign::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u, (first, second) -> first));

        return campaigns.stream()
                .map((campaign) -> {
                    User creator = creators.get(campaign.getUserId());
                    List<String> backerEmails = transactionRepository.findDistinctUserEmailByCampaignId(campaign.getId());
                    int contributionsCount = (int) transactionRepository.countByCampaignId(campaign.getId());
                    return CampaignResponse.fromWithDetails(
                            campaign, categoryNames.get(campaign.getCategoryId()), creator, backerEmails, contributionsCount);
                })
                .toList();
    }

    private Category getDefaultCategory() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hay categorías disponibles en el sistema."));
    }

    private Campaign findCampaign(UUID campaignId) {
        return campaignRepository.findById(campaignId).orElseThrow(CampaignNotFoundException::new);
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidCampaignException(CATEGORY_NOT_FOUND_MESSAGE));
    }

    private String categoryName(UUID categoryId) {
        return categoryRepository.findById(categoryId).map(Category::getName).orElse(null);
    }

    private void validateDeadline(Instant deadline, Instant now) {
        if (!deadline.isAfter(now)) {
            throw new InvalidCampaignException(DEADLINE_NOT_FUTURE_MESSAGE);
        }
        if (deadline.isAfter(now.plus(MAX_DURATION_DAYS, ChronoUnit.DAYS))) {
            throw new InvalidCampaignException(DEADLINE_TOO_FAR_MESSAGE);
        }
    }

    private boolean hasChangedGoal(Campaign campaign, BigDecimal newGoal) {
        return newGoal != null && campaign.getGoal().compareTo(newGoal) != 0;
    }

    private boolean hasChangedDeadline(Campaign campaign, Instant newDeadline) {
        return newDeadline != null && !newDeadline.equals(campaign.getDeadline());
    }

    private List<String> cleanImages(List<String> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream().map(String::trim).toList();
    }

    private List<Reward> toRewards(List<RewardRequest> rewards) {
        if (rewards == null) {
            return List.of();
        }
        return rewards.stream()
                .map(r -> new Reward(r.name().trim(),
                        r.description() == null ? null : r.description().trim(),
                        r.minAmount(), r.stock()))
                .toList();
    }

    private void promoteToCreator(User user, Instant now) {
        if (ROLE_SPONSOR.equals(user.getRole())) {
            user.setRole(ROLE_CREATOR);
            user.setUpdatedAt(now);
            userRepository.save(user);
        }
    }

    @Transactional
    public CampaignResponse updateCampaignStatus(UUID campaignId, CampaignStatus newStatus, UUID userId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException());

        if (!campaign.isOwnedBy(userId)) {
            throw new CampaignAccessDeniedException();
        }

        if (newStatus == CampaignStatus.CLOSED) {
            System.out.println("Notificando a los patrocinadores que la campaña " + campaign.getTitle() + " ha sido cerrada.");
        }

        campaign.setStatus(newStatus);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        User creator = userRepository.findById(updatedCampaign.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        List<String> backerEmails = transactionRepository.findDistinctUserEmailByCampaignId(updatedCampaign.getId());
        int contributionsCount = (int) transactionRepository.countByCampaignId(updatedCampaign.getId());

        return CampaignResponse.fromWithDetails(
                updatedCampaign, categoryName(updatedCampaign.getCategoryId()), creator, backerEmails, contributionsCount);
    }

    @Autowired
    private ContributionEmailService contributionEmailService;

    @Transactional
    public ContributionConfirmationResponse makeContributionWithConfirmation(UUID campaignId,
                                                                              BigDecimal amount,
                                                                              UUID rewardId,
                                                                              UUID userId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidContributionException("El monto a aportar debe ser mayor a cero.");
        }

        Campaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(CampaignNotFoundException::new);
        if (!campaign.isActive()) {
            throw new CampaignNotEditableException(CONTRIBUTION_NOT_ACTIVE_MESSAGE);
        }

        User sponsor = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        Reward reward = rewardId == null ? null : findRewardOf(campaign, rewardId);
        if (reward != null) {
            if (reward.getStock() <= 0) {
                throw new InvalidContributionException("La recompensa seleccionada está agotada.");
            }
            if (amount.compareTo(reward.getMinAmount()) < 0) {
                throw new InvalidContributionException(
                        "El aporte debe ser igual o mayor a " + reward.getMinAmount().toPlainString()
                                + " para elegir la recompensa \"" + reward.getName() + "\".");
            }
        }

        if (sponsor.getBalance().compareTo(amount) < 0) {
            throw new InvalidContributionException("Saldo insuficiente para realizar el aporte.");
        }

        Instant now = clock.instant();
        sponsor.subtractBalance(amount);
        sponsor.setHistoric(sponsor.getHistoric().add(amount));
        sponsor.setUpdatedAt(now);
        campaign.addRaisedAmount(amount);
        if (reward != null) {
            reward.decreaseStock();
        }

        userRepository.save(sponsor);
        campaignRepository.save(campaign);

        Transaction transaction = transactionRepository.saveAndFlush(new Transaction(
                amount,
                now,
                CONTRIBUTION_TYPE,
                sponsor.getId(),
                campaign.getId(),
                campaign.getTitle(),
                reward == null ? null : reward.getId(),
                reward == null ? null : reward.getName()));

        ContributionConfirmationResponse receipt = new ContributionConfirmationResponse(
                transaction.getId(),
                campaign.getId(),
                campaign.getTitle(),
                amount,
                transaction.getRewardId(),
                transaction.getRewardName(),
                sponsor.getBalance(),
                transaction.getDate(),
                CONTRIBUTION_SUCCESS_MESSAGE);

        contributionEmailService.sendConfirmation(sponsor.getEmail(), receipt);
        return receipt;
    }

    private Reward findRewardOf(Campaign campaign, UUID rewardId) {
        return campaign.getRewards().stream()
                .filter(reward -> rewardId.equals(reward.getId()))
                .findFirst()
                .orElseThrow(() -> new InvalidContributionException(
                        "La recompensa seleccionada no pertenece a esta campaña."));
    }
}
