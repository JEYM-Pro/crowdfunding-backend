package co.udea.crowdfunding.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MilestoneEmailListener {

    private final MilestoneEmailService emailService;

    public MilestoneEmailListener(MilestoneEmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMilestoneReached(MilestoneEmailEvent event) {
        emailService.sendMilestoneEmail(
                event.email(),
                event.campaignName(),
                event.percentage());
    }
}
