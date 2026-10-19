package co.udea.crowdfunding.entity;

import co.udea.crowdfunding.exception.CampaignNotEditableException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    public static final String LOCKED_MESSAGE =
            "No puedes modificar la meta ni la fecha límite porque la campaña ya recibió aportes.";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal raised = BigDecimal.ZERO;

    @Column(name = "goal", nullable = false, precision = 12, scale = 2)
    private BigDecimal goal;

    @Column(nullable = false)
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.ACTIVE;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "campaign_images", joinColumns = @JoinColumn(name = "campaign_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private List<String> images = new ArrayList<>();

    /** Recompensas opcionales (máx. 6). Se fijan al publicar y no cambian después. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "campaign_id", nullable = false, updatable = false)
    @OrderBy("minAmount ASC")
    private List<Reward> rewards = new ArrayList<>();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Campaign() {
    }

    /** Fábrica de dominio (HU-06): toda campaña nueva nace activa y sin recaudo. */
    public static Campaign publish(UUID creatorId,
                                   String title,
                                   String description,
                                   BigDecimal goal,
                                   Instant deadline,
                                   UUID categoryId,
                                   List<String> images,
                                   List<Reward> rewards,
                                   Instant now) {
        Campaign campaign = new Campaign();
        campaign.userId = creatorId;
        campaign.title = title;
        campaign.description = description;
        campaign.goal = goal;
        campaign.deadline = deadline;
        campaign.categoryId = categoryId;
        campaign.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        campaign.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        campaign.status = CampaignStatus.ACTIVE;
        campaign.raised = BigDecimal.ZERO;
        campaign.createdAt = now;
        campaign.updatedAt = now;
        return campaign;
    }

    // ---------- Reglas de negocio (HU-07) ----------

    public boolean isActive() {
        return status == CampaignStatus.ACTIVE;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public boolean hasContributions() {
        return raised != null && raised.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Con aportes recibidos, la meta y la fecha límite quedan bloqueadas. */
    public void assertGoalAndDeadlineEditable() {
        if (hasContributions()) {
            throw new CampaignNotEditableException(LOCKED_MESSAGE);
        }
    }

    /** Título, descripción e imágenes siempre se pueden editar mientras la campaña esté activa. */
    public void updateDetails(String title, String description, List<String> newImages, Instant now) {
        this.title = title;
        this.description = description;
        if (newImages != null) {
            this.images.clear();
            this.images.addAll(newImages);
        }
        this.updatedAt = now;
    }

    public void changeCategory(UUID newCategoryId, Instant now) {
        this.categoryId = newCategoryId;
        this.updatedAt = now;
    }

    public void changeGoal(BigDecimal newGoal, Instant now) {
        assertGoalAndDeadlineEditable();
        this.goal = newGoal;
        this.updatedAt = now;
    }

    public void changeDeadline(Instant newDeadline, Instant now) {
        assertGoalAndDeadlineEditable();
        this.deadline = newDeadline;
        this.updatedAt = now;
    }

    // ---------- Getters ----------

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getRaised() { return raised; }
    public BigDecimal getGoal() { return goal; }
    public Instant getDeadline() { return deadline; }
    public CampaignStatus getStatus() { return status; }
    public UUID getCategoryId() { return categoryId; }
    public List<String> getImages() { return Collections.unmodifiableList(images); }
    public List<Reward> getRewards() { return Collections.unmodifiableList(rewards); }
    public UUID getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public void addRaisedAmount(BigDecimal amount) {
        this.raised = this.raised.add(amount);
    }
}
