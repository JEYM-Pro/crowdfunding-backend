package co.udea.crowdfunding.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.crowdfunding.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndType(UUID userId, String type);

    List<Transaction> findByUserIdOrderByDateDesc(UUID userId);

    long countByUserId(UUID userId);

    @Query(nativeQuery = true, value = "SELECT DISTINCT u.email FROM transactions t JOIN users u ON t.user_id = u.id WHERE t.campaign_id = :campaignId")
    List<String> findDistinctUserEmailByCampaignId(@Param("campaignId") UUID campaignId);

    long countByCampaignId(UUID campaignId);

    @Query(nativeQuery = true, value = "SELECT COUNT(DISTINCT t.user_id) FROM transactions t WHERE t.campaign_id = :campaignId")
    long countDistinctContributorsByCampaignId(@Param("campaignId") UUID campaignId);

    List<Transaction> findByCampaignIdAndTypeOrderByDateDesc(UUID campaignId, String type);

    List<Transaction> findByUserIdAndTypeOrderByDateDesc(UUID userId, String type);

    // =======================================================
    // MÉTODOS PARA HU-17 Y HU-19 (SPRINT 3)
    // =======================================================

    // Listar todas las transacciones ordenadas cronológicamente
    List<Transaction> findAllByOrderByDateDesc();

    // HU-17: Consulta con filtros opcionales (fechas, tipo, usuario)
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:startDate IS NULL OR t.date >= :startDate) AND " +
            "(:endDate IS NULL OR t.date <= :endDate) AND " +
            "(:type IS NULL OR LOWER(t.type) = LOWER(:type)) AND " +
            "(:userId IS NULL OR t.userId = :userId) " +
            "ORDER BY t.date DESC")
    List<Transaction> findFilteredTransactions(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("type") String type,
            @Param("userId") UUID userId
    );

// HU-19: Aportes en un rango de fechas (Consolidado Administrador)
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.type) IN ('contribution', 'aporte') " +
            "AND t.date >= :startDate AND t.date <= :endDate " +
            "ORDER BY t.date DESC")
    List<Transaction> findSuccessfulContributionsInPeriod(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    // HU-19: Aportes en un rango de fechas para campañas de un Creador específico
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.type) IN ('contribution', 'aporte') " +
            "AND t.campaignId IN :campaignIds " +
            "AND t.date >= :startDate AND t.date <= :endDate " +
            "ORDER BY t.date DESC")
    List<Transaction> findContributionsByCampaignIdsInPeriod(
            @Param("campaignIds") List<UUID> campaignIds,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}