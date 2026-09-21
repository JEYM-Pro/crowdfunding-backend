package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.BalanceResponse;
import co.udea.crowdfunding.dto.CampaignSummary;
import co.udea.crowdfunding.dto.RegisterRequest;
import co.udea.crowdfunding.dto.TransactionItem;
import co.udea.crowdfunding.dto.UpdateProfileRequest;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.EmailAlreadyExistsException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String ROLE_CREATOR = "creator";
    private static final String CONTRIBUTION_TYPE = "contribution";

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CampaignRepository campaignRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                        TransactionRepository transactionRepository,
                        CampaignRepository campaignRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.campaignRepository = campaignRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()));

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException();
        }
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String newEmail = request.email().trim().toLowerCase();

        Optional<User> existingUser = userRepository.findByEmail(newEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new EmailAlreadyExistsException();
        }

        user.setName(request.name().trim());
        user.setEmail(newEmail);
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    @Transactional
    public User recharge(UUID userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setBalance(user.getBalance().add(amount));
        user.setUpdatedAt(Instant.now());

        Transaction transaction = new Transaction(amount, Instant.now(), "recharge", userId, null, null);
        transactionRepository.save(transaction);

        return userRepository.save(user);
    }

    public BalanceResponse getBalanceDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<CampaignSummary> campaigns = ROLE_CREATOR.equals(user.getRole())
                ? buildCreatorCampaigns(userId)
                : buildSponsorCampaigns(userId);

        List<TransactionItem> history = transactionRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(tx -> new TransactionItem(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getType(),
                        tx.getDate(),
                        tx.getCampaignName(),
                        tx.getCampaignId()))
                .toList();

        return new BalanceResponse(user.getBalance(), user.getHistoric(), campaigns, history);
    }

    private List<CampaignSummary> buildCreatorCampaigns(UUID userId) {
        return campaignRepository.findByUserId(userId).stream()
                .map(campaign -> new CampaignSummary(
                        campaign.getId(),
                        campaign.getTitle(),
                        campaign.getRaised(),
                        campaign.getGoal(),
                        BigDecimal.ZERO))
                .toList();
    }

    private List<CampaignSummary> buildSponsorCampaigns(UUID userId) {
        List<Transaction> contributions = transactionRepository.findByUserIdAndType(userId, CONTRIBUTION_TYPE);

        Map<UUID, BigDecimal> contributedByCampaign = contributions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCampaignId,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        if (contributedByCampaign.isEmpty()) {
            return List.of();
        }

        Map<UUID, Campaign> campaignsById = campaignRepository.findAllById(contributedByCampaign.keySet()).stream()
                .collect(Collectors.toMap(Campaign::getId, campaign -> campaign));

        return contributedByCampaign.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(entry -> {
                    Campaign campaign = campaignsById.get(entry.getKey());
                    return new CampaignSummary(
                            campaign.getId(),
                            campaign.getTitle(),
                            campaign.getRaised(),
                            campaign.getGoal(),
                            entry.getValue());
                })
                .toList();
    }
}