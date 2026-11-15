package co.udea.crowdfunding.service;

import co.udea.crowdfunding.entity.VerificationAttempt;
import co.udea.crowdfunding.entity.VerificationLockout;
import co.udea.crowdfunding.repository.VerificationAttemptRepository;
import co.udea.crowdfunding.repository.VerificationLockoutRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * HU-S07: limita los intentos fallidos al ingresar la contraseña de la cuenta o el PIN de una campaña.
 * Tras {@code maxAttempts} fallos consecutivos bloquea esa verificación durante {@code lockDuration}
 * y registra el bloqueo; una verificación exitosa reinicia el contador.
 */
@Service
public class VerificationAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationAttemptService.class);

    private final VerificationAttemptRepository attemptRepository;
    private final VerificationLockoutRepository lockoutRepository;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;

    public VerificationAttemptService(
            VerificationAttemptRepository attemptRepository,
            VerificationLockoutRepository lockoutRepository,
            Clock clock,
            @Value("${app.security.verification.max-attempts:5}") int maxAttempts,
            @Value("${app.security.verification.lock-minutes:15}") long lockMinutes) {
        if (maxAttempts < 1 || lockMinutes < 1) {
            throw new IllegalStateException("El límite de intentos y el tiempo de bloqueo deben ser mayores a cero.");
        }
        this.attemptRepository = attemptRepository;
        this.lockoutRepository = lockoutRepository;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    /**
     * Ejecuta {@code check} salvo que la verificación esté bloqueada.
     * Corre en su propia transacción para que el fallo quede contado aunque la operación que lo
     * pidió se revierta al lanzar su error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationResult verify(UUID userId, VerificationScope scope, BooleanSupplier check) {
        Instant now = clock.instant();
        VerificationAttempt attempt = attemptRepository.findByUserIdAndScope(userId, scope.key())
                .orElseGet(() -> new VerificationAttempt(userId, scope.key(), now));

        // Bloqueada: ni siquiera se comprueba el dato, así un ataque no gana información.
        if (attempt.isLockedAt(now)) {
            return VerificationResult.locked(attempt.remainingLock(now));
        }

        if (check.getAsBoolean()) {
            if (attempt.hasFailures()) {
                attempt.reset(now);
                attemptRepository.save(attempt);
            }
            return VerificationResult.ok();
        }

        attempt.registerFailure(now);
        if (attempt.getFailedAttempts() >= maxAttempts) {
            Instant lockedUntil = now.plus(lockDuration);
            lockoutRepository.save(new VerificationLockout(
                    userId, scope.campaignId(), scope.key(), attempt.getFailedAttempts(), now, lockedUntil));
            attempt.lockUntil(lockedUntil, now);
            attemptRepository.save(attempt);
            logger.warn("Verificación bloqueada por intentos fallidos: userId={}, campaignId={}, hasta={}",
                    userId, scope.campaignId(), lockedUntil);
            return VerificationResult.locked(lockDuration);
        }

        attemptRepository.save(attempt);
        return VerificationResult.failed();
    }
}
