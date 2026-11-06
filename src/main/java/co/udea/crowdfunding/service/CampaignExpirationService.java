package co.udea.crowdfunding.service;

import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.CampaignStatusHistory;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.CampaignStatusHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CampaignExpirationService {

    private final CampaignRepository campaignRepository;
    private final CampaignStatusHistoryRepository statusHistoryRepository;
    private final Clock clock;

    public CampaignExpirationService(
            CampaignRepository campaignRepository,
            CampaignStatusHistoryRepository statusHistoryRepository,
            Clock clock) {
        this.campaignRepository = campaignRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void finishExpiredCampaigns() {
        Instant now = clock.instant();

        List<Campaign> expiredCampaigns =
                campaignRepository.findByStatusAndDeadlineLessThanEqual(
                        CampaignStatus.ACTIVE, now);

        for (Campaign campaign : expiredCampaigns) {
            campaign.setStatus(CampaignStatus.FINISHED);
            campaignRepository.save(campaign);

            statusHistoryRepository.save(
                    new CampaignStatusHistory(
                            campaign.getId(),
                            CampaignStatus.ACTIVE,
                            CampaignStatus.FINISHED,
                            campaign.getUserId(),
                            now));
        }
    }
}
