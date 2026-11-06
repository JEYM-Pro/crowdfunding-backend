package co.udea.crowdfunding.service;

import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignMilestone;
import co.udea.crowdfunding.entity.Notification;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.repository.CampaignMilestoneRepository;
import co.udea.crowdfunding.repository.NotificationRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CampaignMilestoneService {

    private static final int[] MILESTONES = {25, 50, 75, 100};
    private static final String CONTRIBUTION_TYPE = "contribution";

    private final CampaignMilestoneRepository milestoneRepository;
    private final NotificationRepository notificationRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CampaignMilestoneService(
            CampaignMilestoneRepository milestoneRepository,
            NotificationRepository notificationRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.milestoneRepository = milestoneRepository;
        this.notificationRepository = notificationRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public void checkMilestones(
            Campaign campaign, BigDecimal previousRaised, Instant now) {

        if (campaign.getGoal().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal previousPercentage = previousRaised
                .multiply(BigDecimal.valueOf(100))
                .divide(campaign.getGoal(), 4, java.math.RoundingMode.HALF_UP);

        BigDecimal currentPercentage = campaign.getRaised()
                .multiply(BigDecimal.valueOf(100))
                .divide(campaign.getGoal(), 4, java.math.RoundingMode.HALF_UP);

        Set<UUID> recipients = new HashSet<>();
        recipients.add(campaign.getUserId());

        List<Transaction> transactions =
                transactionRepository.findByCampaignIdAndTypeOrderByDateDesc(
                        campaign.getId(), CONTRIBUTION_TYPE);

        for (Transaction transaction : transactions) {
            recipients.add(transaction.getUserId());
        }

        List<User> users = userRepository.findAllById(recipients);

        for (int milestone : MILESTONES) {
            BigDecimal target = BigDecimal.valueOf(milestone);

            if (previousPercentage.compareTo(target) < 0
                    && currentPercentage.compareTo(target) >= 0
                    && !milestoneRepository.existsByCampaignIdAndPercentage(
                            campaign.getId(), milestone)) {

                milestoneRepository.save(new CampaignMilestone(
                        campaign.getId(), milestone, now));

                String title = "Meta de " + milestone + "% alcanzada";
                String message = "La campaña \"" + campaign.getTitle()
                        + "\" alcanzó el " + milestone
                        + "% de su objetivo de recaudo.";

                for (User user : users) {
                    notificationRepository.save(new Notification(
                            user.getId(),
                            campaign.getId(),
                            title,
                            message,
                            now));

                    if (user.getEmail() != null
                            && !user.getEmail().isBlank()) {
                        eventPublisher.publishEvent(new MilestoneEmailEvent(
                                user.getEmail(),
                                campaign.getTitle(),
                                milestone));
                    }
                }
            }
        }
    }
}
