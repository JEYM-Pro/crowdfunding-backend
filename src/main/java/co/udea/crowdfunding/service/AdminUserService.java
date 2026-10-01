package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.AdminCreateUserRequest;
import co.udea.crowdfunding.dto.AdminUpdateUserRequest;
import co.udea.crowdfunding.dto.AdminUserActionRequest;
import co.udea.crowdfunding.dto.AdminUserDetailResponse;
import co.udea.crowdfunding.dto.AdminUserListRequest;
import co.udea.crowdfunding.dto.AdminUserListResponse;
import co.udea.crowdfunding.dto.AdminUserSummary;
import co.udea.crowdfunding.entity.Campaign;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.exception.EmailAlreadyExistsException;
import co.udea.crowdfunding.repository.CampaignRepository;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private static final String ROLE_SPONSOR = "sponsor";
    private static final String ROLE_CREATOR = "creator";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            CampaignRepository campaignRepository,
                            TransactionRepository transactionRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminUserListResponse listUsers(AdminUserListRequest request) {
        Pageable pageable = buildPageable(request);
        Page<User> userPage = userRepository.findByFilters(
                request.getEmail(),
                request.getRole(),
                request.getStatus(),
                pageable
        );

        Page<AdminUserSummary> summaryPage = userPage.map(this::toSummary);
        return new AdminUserListResponse(summaryPage);
    }

    public AdminUserDetailResponse getUserDetail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        long campaignsCount = campaignRepository.countByUserId(userId);
        long transactionsCount = transactionRepository.countByUserId(userId);

        return new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getBalance(),
                user.getHistoric(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                campaignsCount,
                transactionsCount
        );
    }

    @Transactional
    public AdminUserDetailResponse createUser(AdminCreateUserRequest request, UUID adminId) {
        String email = request.getEmail();
        String role = request.getRole();

        if (!ROLE_SPONSOR.equals(role) && !ROLE_CREATOR.equals(role)) {
            throw new IllegalArgumentException("El rol debe ser 'sponsor' o 'creator'");
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(
                request.getName(),
                email,
                passwordEncoder.encode(request.password())
        );
        user.setRole(role);
        user.setStatus(STATUS_ACTIVE);

        try {
            User saved = userRepository.save(user);
            return toDetailResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException();
        }
    }

    @Transactional
    public AdminUserDetailResponse updateUser(UUID userId, AdminUpdateUserRequest request, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request.hasEmail()) {
            String newEmail = request.getEmail();
            if (userRepository.existsByEmail(newEmail)) {
                var existing = userRepository.findByEmail(newEmail);
                if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                    throw new EmailAlreadyExistsException();
                }
            }
            user.setEmail(newEmail);
        }

        if (request.hasName()) {
            user.setName(request.getName());
        }

        if (request.hasRole()) {
            String newRole = request.getRole();
            if (!ROLE_SPONSOR.equals(newRole) && !ROLE_CREATOR.equals(newRole)) {
                throw new IllegalArgumentException("El rol debe ser 'sponsor' o 'creator'");
            }
            user.setRole(newRole);
        }

        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);
        return toDetailResponse(saved);
    }

    @Transactional
    public void changeStatus(UUID userId, AdminUserActionRequest request, UUID adminId) {
        if (userId.equals(adminId)) {
            throw new IllegalStateException("No puedes cambiar el estado de tu propia cuenta");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String action = request.getAction();
        if ("DISABLE".equals(action)) {
            user.setStatus(STATUS_INACTIVE);
        } else if ("ENABLE".equals(action)) {
            user.setStatus(STATUS_ACTIVE);
        } else {
            throw new IllegalArgumentException("Acción inválida: debe ser DISABLE o ENABLE");
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private Pageable buildPageable(AdminUserListRequest request) {
        String[] sortParts = request.getSort().split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(request.getPage(), request.getSize(), Sort.by(direction, sortField));
    }

    private AdminUserSummary toSummary(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private AdminUserDetailResponse toDetailResponse(User user) {
        long campaignsCount = campaignRepository.countByUserId(user.getId());
        long transactionsCount = transactionRepository.countByUserId(user.getId());

        return new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getBalance(),
                user.getHistoric(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                campaignsCount,
                transactionsCount
        );
    }
}