
package co.udea.crowdfunding.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.crowdfunding.dto.TransactionDTO;
import co.udea.crowdfunding.entity.Transaction;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.repository.TransactionRepository;
import co.udea.crowdfunding.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class TransactionQueryService {

    private static final String DEFAULT_STATUS = "EXITOSA";

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionQueryService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Consulta general de transacciones con filtros opcionales (HU-17).
     * Solo lectura garantizada por @Transactional(readOnly = true).
     */
    public List<TransactionDTO> getTransactions(Instant startDate, Instant endDate, String type, UUID userId) {
        List<Transaction> transactions = transactionRepository.findAll(
                filters(startDate, endDate, type, userId), Sort.by(Sort.Direction.DESC, "date"));

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName, (existente, reemplazo) -> existente));

        return transactions.stream()
                .map(t -> mapToDTO(t, userNames))
                .collect(Collectors.toList());
    }

    /** Solo agrega al WHERE los filtros que vienen informados. */
    static Specification<Transaction> filters(Instant startDate, Instant endDate, String type, UUID userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("type")), type.trim().toLowerCase()));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TransactionDTO mapToDTO(Transaction t, Map<UUID, String> userNames) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(t.getId());
        dto.setDate(t.getDate());
        dto.setType(t.getType());
        dto.setAmount(t.getAmount());

        String userName = (t.getUserId() != null) ? userNames.getOrDefault(t.getUserId(), "Usuario no encontrado") : "N/A";
        dto.setUserName(userName);

        dto.setStatus(DEFAULT_STATUS);
        return dto;
    }
}
