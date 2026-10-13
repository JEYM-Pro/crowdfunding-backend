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
}
