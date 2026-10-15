package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    /** HU-11: bloquea la campaña para serializar aportes y no consumir dos veces la última unidad de una recompensa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Campaign c WHERE c.id = :campaignId")
    Optional<Campaign> findByIdForUpdate(@Param("campaignId") UUID campaignId);

    List<Campaign> findByStatusOrderByCreatedAtDesc(CampaignStatus status);

    // Método para la HU-09: Buscar por nombre parcial (ignorando mayúsculas/minúsculas) y por categoría, ordenado
    List<Campaign> findByTitleContainingIgnoreCaseAndCategoryIdAndStatus(String title, UUID categoryId, CampaignStatus status, org.springframework.data.domain.Sort sort);

    // Método alternativo si el usuario solo filtra por nombre y no por categoría
    List<Campaign> findByTitleContainingIgnoreCaseAndStatus(String title, CampaignStatus status, org.springframework.data.domain.Sort sort);

    // Método alternativo si el usuario solo filtra por categoría
    List<Campaign> findByCategoryIdAndStatus(UUID categoryId, CampaignStatus status, org.springframework.data.domain.Sort sort);

    // Método base para traer todas las activas pero permitiendo ordenamiento
    List<Campaign> findByStatus(CampaignStatus status, org.springframework.data.domain.Sort sort);
}
