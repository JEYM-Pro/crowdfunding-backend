package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Page<User> findByFilters(String email, String role, String status, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t WHERE t.userId = :userId")
    boolean hasTransactions(UUID userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Campaign c WHERE c.userId = :userId")
    boolean hasCampaigns(UUID userId);

    default boolean hasFinancialRecords(UUID userId) {
        return hasTransactions(userId) || hasCampaigns(userId);
    }
}