package co.udea.crowdfunding.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.udea.crowdfunding.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

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

    // HU-17: los filtros opcionales se arman con Specification en TransactionQueryService.
    // Un JPQL con "(:param IS NULL OR ...)" falla en PostgreSQL cuando el parámetro llega nulo
    // (no puede deducir su tipo), por eso no se usa aquí.

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