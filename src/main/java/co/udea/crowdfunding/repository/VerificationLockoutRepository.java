package co.udea.crowdfunding.repository;

import co.udea.crowdfunding.entity.VerificationLockout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VerificationLockoutRepository extends JpaRepository<VerificationLockout, UUID> {

    List<VerificationLockout> findByUserIdOrderByLockedAtDesc(UUID userId);
}
