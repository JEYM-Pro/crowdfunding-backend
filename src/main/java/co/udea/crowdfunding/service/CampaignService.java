package co.udea.crowdfunding.service;

import org.springframework.data.domain.Sort;
import co.udea.crowdfunding.dto.CampaignResponse;
import co.udea.crowdfunding.dto.CreateCampaignRequest;
import co.udea.crowdfunding.dto.RewardRequest;
import co.udea.crowdfunding.dto.UpdateCampaignRequest;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.CampaignStatusHistory;
import co.udea.crowdfunding.entity.Category;
import co.udea.crowdfunding.entity.Reward;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.CampaignAccessDeniedException;
import co.udea.crowdfunding.exception.CampaignNotEditableException;
import co.udea.crowdfunding.exception.CampaignNotFoundException;
import co.udea.crowdfunding.exception.InvalidCampaignException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.CampaignStatusHistoryRepository;
import co.udea.crowdfunding.repository.CategoryRepository;
import co.udea.crowdfunding.repository.UserRepository;
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

/**
 * Casos de uso de campañas: HU-06 (publicar) y HU-07 (editar campaña activa).
 */
@Service
@Transactional(readOnly = true)
public class CampaignService {

    static final int MAX_DURATION_DAYS = 90;
    static final String DEADLINE_NOT_FUTURE_MESSAGE = "La fecha límite debe ser posterior a hoy.";
    static final String DEADLINE_TOO_FAR_MESSAGE = "La fecha límite no puede superar los 90 días.";
    static final String CATEGORY_NOT_FOUND_MESSAGE = "La categoría seleccionada no existe.";
    static final String NOT_ACTIVE_MESSAGE = "Solo se pueden editar campañas activas.";
    static final String REWARDS_LOCKED_MESSAGE = "La lista de recompensas no se puede modificar una vez publicada la campaña.";

    private static final String ROLE_SPONSOR = "sponsor";
    private static final String ROLE_CREATOR = "creator";
    private final CampaignRepository campaignRepository;
    private final CategoryRepository categoryRepository;
    private final CampaignStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public CampaignService(CampaignRepository campaignRepository,
                           CategoryRepository categoryRepository,
                           CampaignStatusHistoryRepository statusHistoryRepository,
                           UserRepository userRepository,
                           Clock clock) {
        this.campaignRepository = campaignRepository;
        this.categoryRepository = categoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- HU-06

    @Transactional
    public CampaignResponse create(UUID userId, CreateCampaignRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Instant now = clock.instant();
        validateDeadline(request.deadline(), now);
        Category category = findCategory(request.categoryId());

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

        return CampaignResponse.from(campaign, category.getName());
    }

    // ---------------------------------------------------------------- HU-07

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
        return CampaignResponse.from(saved, categoryName(saved.getCategoryId()));
    }

    // ------------------------------------------------------------- Consultas

    public CampaignResponse getById(UUID campaignId) {
        Campaign campaign = findCampaign(campaignId);
        return CampaignResponse.from(campaign, categoryName(campaign.getCategoryId()));
    }

    /** Listado público: solo campañas activas, las más recientes primero. */
    public List<CampaignResponse> listActive() {
        List<Campaign> campaigns = campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE);

        Map<UUID, String> categoryNames = categoryRepository
                .findAllById(campaigns.stream().map(Campaign::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (first, second) -> first));

        return campaigns.stream()
                .map(campaign -> CampaignResponse.from(campaign, categoryNames.get(campaign.getCategoryId())))
                .toList();
    }

    /** HU-09: Explorar y filtrar campañas activas */
    public List<CampaignResponse> exploreActiveCampaigns(String nameFilter, UUID categoryIdFilter, String sortBy) {
        
        // 1. Determinar el criterio de ordenamiento
        Sort sortCriteria = Sort.by(Sort.Direction.DESC, "createdAt"); // Por defecto: más recientes

        if (sortBy != null && !sortBy.isBlank()) {
            switch (sortBy.toLowerCase()) {
                case "recaudo":
                    sortCriteria = Sort.by(Sort.Direction.DESC, "raised"); // Mayor recaudo primero
                    break;
                case "fecha":
                    sortCriteria = Sort.by(Sort.Direction.ASC, "deadline"); // Próximas a vencer primero
                    break;
                case "popularidad":
                    // Asumiremos que popularidad es igual a recaudo por ahora, o podrías usar la cantidad de transacciones si la tuvieras
                    sortCriteria = Sort.by(Sort.Direction.DESC, "raised"); 
                    break;
            }
        }

        // 2. Ejecutar la búsqueda combinando los filtros
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

        // 3. Mapear las entidades encontradas a DTOs (igual que lo hacía tu compañero)
        Map<UUID, String> categoryNames = categoryRepository
                .findAllById(campaigns.stream().map(Campaign::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (first, second) -> first));

        return campaigns.stream()
                .map(campaign -> CampaignResponse.from(campaign, categoryNames.get(campaign.getCategoryId())))
                .toList();
    }

    // --------------------------------------------------------------- Helpers

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

    /** Quien publica su primera campaña pasa de patrocinador a creador (los demás roles no cambian). */
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

        // Validación de permisos: Usamos el método isOwnedBy que ya trae la entidad
        if (!campaign.isOwnedBy(userId)) {
            throw new CampaignAccessDeniedException();
        }

        // Si se cierra, simulamos la notificación a patrocinadores requerida por la HU-08
        if (newStatus == CampaignStatus.CLOSED) {
            System.out.println("Notificando a los patrocinadores que la campaña " + campaign.getTitle() + " ha sido cerrada.");
        }

        campaign.setStatus(newStatus);
        Campaign updatedCampaign = campaignRepository.save(campaign);

        // Retornamos la respuesta
        return CampaignResponse.from(updatedCampaign, categoryName(updatedCampaign.getCategoryId()));
    }

    @org.springframework.beans.factory.annotation.Autowired
    private co.udea.crowdfunding.repository.TransactionRepository transactionRepository;

    @Transactional
    public void makeContribution(UUID campaignId, java.math.BigDecimal amount, UUID userId) {
        // 1. Validar que el monto sea mayor a cero
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a aportar debe ser mayor a cero.");
        }

        // 2. Buscar la campaña y validar que esté activa
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException());
        
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new IllegalStateException("No se pueden realizar aportes a una campaña que no está activa.");
        }

        // 3. Buscar al usuario patrocinador
        User sponsor = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // 4. Descontar saldo y sumar recaudo
        sponsor.subtractBalance(amount);
        campaign.addRaisedAmount(amount);

        // 5. Guardar los cambios
        userRepository.save(sponsor);
        campaignRepository.save(campaign);

        // 6. Registrar la transacción en el historial con los 6 parámetros correctos
        co.udea.crowdfunding.entity.Transaction transaction = new co.udea.crowdfunding.entity.Transaction(
            amount, 
            clock.instant(), 
            "CONTRIBUTION", 
            sponsor.getId(), 
            campaign.getId(), 
            campaign.getTitle()
        );
        transactionRepository.save(transaction);
    }

}

