package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.CampaignContributionResponse;
import co.udea.crowdfunding.dto.CampaignEarningsResponse;
import co.udea.crowdfunding.dto.SponsorContributionResponse;
import co.udea.crowdfunding.dto.SponsorContributionsSummaryResponse;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.CampaignAccessDeniedException;
import co.udea.crowdfunding.exception.CampaignNotFoundException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContributionQueryService {

    private static final String CONTRIBUTION_TYPE = "contribution";

    private final CampaignRepository campaignRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public ContributionQueryService(
            CampaignRepository campaignRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.campaignRepository = campaignRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public CampaignEarningsResponse getCampaignEarnings(
            UUID campaignId,
            UUID userId,
            String contributorFilter,
            String rewardFilter) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(CampaignNotFoundException::new);

        if (!campaign.isOwnedBy(userId)) {
            throw new CampaignAccessDeniedException();
        }

        List<Transaction> transactions =
                transactionRepository.findByCampaignIdAndTypeOrderByDateDesc(
                        campaignId, CONTRIBUTION_TYPE);

        Map<UUID, User> contributors = userRepository.findAllById(
                transactions.stream()
                        .map(Transaction::getUserId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        User::getId, Function.identity()));

        List<CampaignContributionResponse> contributions = transactions.stream()
                .filter(transaction -> {
                    User contributor = contributors.get(transaction.getUserId());

                    return contributorFilter == null
                            || contributorFilter.isBlank()
                            || transaction.getUserId().toString()
                                    .equalsIgnoreCase(contributorFilter)
                            || (contributor != null
                                    && contributor.getEmail() != null
                                    && contributor.getEmail().toLowerCase()
                                            .contains(contributorFilter.toLowerCase()));
                })
                .filter(transaction -> rewardFilter == null
                        || rewardFilter.isBlank()
                        || (transaction.getRewardName() != null
                                && transaction.getRewardName().toLowerCase()
                                        .contains(rewardFilter.toLowerCase())))
                .map(transaction -> {
                    User contributor = contributors.get(transaction.getUserId());

                    return new CampaignContributionResponse(
                            transaction.getId(),
                            transaction.getUserId(),
                            null,
                            contributor == null ? null : contributor.getEmail(),
                            transaction.getAmount(),
                            transaction.getDate(),
                            transaction.getRewardId(),
                            transaction.getRewardName());
                })
                .toList();

        return new CampaignEarningsResponse(
                campaign.getId(),
                campaign.getTitle(),
                campaign.getRaised(),
                transactionRepository.countDistinctContributorsByCampaignId(campaignId),
                contributions);
    }

    public SponsorContributionsSummaryResponse getSponsorSummary(UUID userId) {

        List<Transaction> transactions =
                transactionRepository.findByUserIdAndTypeOrderByDateDesc(
                        userId, CONTRIBUTION_TYPE);

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCampaignId));

        List<SponsorContributionResponse> campaigns = grouped.entrySet().stream()
                .map(entry -> {
                    List<Transaction> campaignTransactions = entry.getValue();

                    BigDecimal campaignTotal = campaignTransactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new SponsorContributionResponse(
                            entry.getKey(),
                            campaignTransactions.get(0).getCampaignName(),
                            campaignTotal);
                })
                .toList();

        return new SponsorContributionsSummaryResponse(total, campaigns);
    }
}
