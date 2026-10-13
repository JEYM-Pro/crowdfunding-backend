package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.CampaignStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignStatusHistoryRepository extends JpaRepository<CampaignStatusHistory, UUID> {

    List<CampaignStatusHistory> findByCampaignIdOrderByChangedAtAsc(UUID campaignId);
}
