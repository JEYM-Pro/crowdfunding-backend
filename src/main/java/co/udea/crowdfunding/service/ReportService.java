package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ActiveCampaignReportDTO;
import co.udea.crowdfunding.dto.PeriodFundsReportDTO;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final CampaignRepository campaignRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(CampaignRepository campaignRepository, TransactionRepository transactionRepository) {
        this.campaignRepository = campaignRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * HU-18: Reporte de campañas activas con cálculo de alerta.
     */
    public List<ActiveCampaignReportDTO> getActiveCampaignsReport(UUID userId, boolean isAdmin) {
        List<Campaign> campaigns;

        if (isAdmin) {
            campaigns = campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE);
        } else {
            campaigns = campaignRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .filter(Campaign::isActive)
                    .toList();
        }

        Instant now = Instant.now();
        return campaigns.stream()
                .map(campaign -> toActiveReportDTO(campaign, now))
                .toList();
    }

    /**
     * HU-19: Reporte de fondos recaudados por periodo.
     * Si la fecha final es anterior a la inicial, rechaza la operación con error.
     * Si no hay transacciones en el periodo, devuelve mensaje indicativo.
     */
    public PeriodFundsReportDTO getFundsByPeriod(Instant startDate, Instant endDate, UUID userId, boolean isAdmin) {
        // Validación obligatoria de la HU-19
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }

        List<Transaction> contributions;

        if (isAdmin) {
            contributions = transactionRepository.findSuccessfulContributionsInPeriod(startDate, endDate);
        } else {
            List<UUID> campaignIds = campaignRepository.findByUserId(userId).stream()
                    .map(Campaign::getId)
                    .toList();

            if (campaignIds.isEmpty()) {
                return new PeriodFundsReportDTO(
                        BigDecimal.ZERO,
                        0,
                        BigDecimal.ZERO,
                        "No hay transacciones para dicho periodo."
                );
            }
            contributions = transactionRepository.findContributionsByCampaignIdsInPeriod(campaignIds, startDate, endDate);
        }

        if (contributions.isEmpty()) {
            return new PeriodFundsReportDTO(
                    BigDecimal.ZERO,
                    0,
                    BigDecimal.ZERO,
                    "No hay transacciones para dicho periodo."
            );
        }

        BigDecimal total = contributions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = contributions.size();
        BigDecimal average = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        return new PeriodFundsReportDTO(
                total,
                count,
                average,
                "Reporte generado exitosamente."
        );
    }

    private ActiveCampaignReportDTO toActiveReportDTO(Campaign campaign, Instant now) {
        BigDecimal goal = campaign.getGoal();
        BigDecimal raised = campaign.getRaised() != null ? campaign.getRaised() : BigDecimal.ZERO;

        double percentage = 0.0;
        if (goal != null && goal.compareTo(BigDecimal.ZERO) > 0) {
            percentage = raised.multiply(BigDecimal.valueOf(100))
                    .divide(goal, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long daysRemaining = 0;
        if (campaign.getDeadline() != null && campaign.getDeadline().isAfter(now)) {
            daysRemaining = Duration.between(now, campaign.getDeadline()).toDays();
        }

        boolean alert = daysRemaining <= 5 && percentage < 50.0;

        return new ActiveCampaignReportDTO(
                campaign.getId(),
                campaign.getTitle(),
                goal,
                raised,
                percentage,
                daysRemaining,
                alert
        );
    }
}