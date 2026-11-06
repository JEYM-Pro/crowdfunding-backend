package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.CampaignMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignMilestoneRepository extends JpaRepository<CampaignMilestone, UUID> {

    boolean existsByCampaignIdAndPercentage(UUID campaignId, int percentage);
}
