package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByUserId(UUID userId);

    long countByUserId(UUID userId);

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
