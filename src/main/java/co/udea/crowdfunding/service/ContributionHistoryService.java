package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.ContributionHistoryResponse;
import co.udea.crowdfunding.dto.ContributionHistorySummaryResponse;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ContributionHistoryService {

    private static final String CONTRIBUTION_TYPE = "contribution";
    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private final TransactionRepository transactionRepository;

    public ContributionHistoryService(
            TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public ContributionHistorySummaryResponse getHistory(UUID userId) {

        List<Transaction> transactions =
                transactionRepository.findByUserIdAndTypeOrderByDateDesc(
                        userId, CONTRIBUTION_TYPE);

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
                                CONFIRMED_STATUS))
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
