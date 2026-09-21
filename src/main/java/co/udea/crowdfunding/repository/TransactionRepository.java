package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndType(UUID userId, String type);

    List<Transaction> findByUserIdOrderByDateDesc(UUID userId);
}
