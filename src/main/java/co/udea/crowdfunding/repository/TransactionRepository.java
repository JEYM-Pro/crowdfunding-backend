package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndType(UUID userId, String type);

    List<Transaction> findByUserIdOrderByDateDesc(UUID userId);

    long countByUserId(UUID userId);

    @Query(nativeQuery = true, value = "SELECT DISTINCT u.email FROM transactions t JOIN users u ON t.user_id = u.id WHERE t.campaign_id = :campaignId AND t.type = 'contribution'")
    List<String> findDistinctUserEmailByCampaignId(@Param("campaignId") UUID campaignId);

    long countByCampaignId(UUID campaignId);

    @Query(nativeQuery = true, value = "SELECT COUNT(DISTINCT t.user_id) FROM transactions t WHERE t.campaign_id = :campaignId AND t.type = 'contribution'")
    long countDistinctContributorsByCampaignId(@Param("campaignId") UUID campaignId);

    List<Transaction> findByCampaignIdAndTypeOrderByDateDesc(UUID campaignId, String type);

    List<Transaction> findByUserIdAndTypeOrderByDateDesc(UUID userId, String type);
}
