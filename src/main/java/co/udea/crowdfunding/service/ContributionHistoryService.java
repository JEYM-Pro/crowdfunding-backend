package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ContributionHistoryResponse;
import co.udea.crowdfunding.dto.ContributionHistorySummaryResponse;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContributionHistoryService {

    private static final String CONTRIBUTION_TYPE = "contribution";

    private final TransactionRepository transactionRepository;
    private final CampaignRepository campaignRepository;

    public ContributionHistoryService(
            TransactionRepository transactionRepository,
            CampaignRepository campaignRepository) {
        this.transactionRepository = transactionRepository;
        this.campaignRepository = campaignRepository;
    }

    public ContributionHistorySummaryResponse getHistory(UUID userId) {

        List<Transaction> transactions =
                transactionRepository.findByUserIdAndTypeOrderByDateDesc(
                        userId, CONTRIBUTION_TYPE);

        // HU-16: estado actual de cada campaña, consultado en un solo viaje a la base de datos.
        Map<UUID, CampaignStatus> campaignStatuses = campaignRepository
                .findAllById(transactions.stream()
                        .map(Transaction::getCampaignId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Campaign::getId, Campaign::getStatus));

        List<ContributionHistoryResponse> contributions =
                transactions.stream()
                        .map(transaction -> new ContributionHistoryResponse(
                                transaction.getId(),
                                transaction.getCampaignId(),
                                transaction.getCampaignName(),
                                transaction.getAmount(),
                                transaction.getDate(),
                                transaction.getRewardId(),
                                transaction.getRewardName(),
                                campaignStatuses.get(transaction.getCampaignId())))
                        .toList();

        BigDecimal totalContributed = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ContributionHistorySummaryResponse(
                totalContributed,
                contributions.size(),
                contributions);
    }
}
