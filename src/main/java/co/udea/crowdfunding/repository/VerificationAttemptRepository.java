package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.VerificationAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface VerificationAttemptRepository extends JpaRepository<VerificationAttempt, UUID> {

    /** HU-S07: bloquea la fila para que dos intentos simultáneos no se cuenten como uno solo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationAttempt> findByUserIdAndScope(UUID userId, String scope);
}
