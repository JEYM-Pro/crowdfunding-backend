package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    /** HU-11: bloquea al usuario para que solicitudes simultáneas no gasten el mismo saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    @Query("""
            SELECT u FROM User u
            WHERE (CAST(:email AS String) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS String), '%')))
              AND (CAST(:role AS String) IS NULL OR u.role = :role)
              AND (CAST(:status AS String) IS NULL OR u.status = :status)
            """)
    Page<User> findByFilters(@Param("email") String email,
                             @Param("role") String role,
                             @Param("status") String status,
                             Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t WHERE t.userId = :userId")
    boolean hasTransactions(UUID userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Campaign c WHERE c.userId = :userId")
    boolean hasCampaigns(UUID userId);

    default boolean hasFinancialRecords(UUID userId) {
        return hasTransactions(userId) || hasCampaigns(userId);
    }
}