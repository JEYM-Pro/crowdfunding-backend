package co.udea.crowdfunding.service;

import co.udea.crowdfunding.entity.VerificationAttempt;
import co.udea.crowdfunding.entity.VerificationLockout;
import co.udea.crowdfunding.exception.InvalidPasswordException;
import co.udea.crowdfunding.exception.VerificationLockedException;
import co.udea.crowdfunding.repository.VerificationAttemptRepository;
import co.udea.crowdfunding.repository.VerificationLockoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-S07: bloqueo temporal tras varios intentos fallidos de verificación. */
@ExtendWith(MockitoExtension.class)
class VerificationAttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final VerificationScope CAMPAIGN_SCOPE = VerificationScope.campaign(CAMPAIGN_ID);
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    @Mock
    private VerificationAttemptRepository attemptRepository;
    @Mock
    private VerificationLockoutRepository lockoutRepository;

    private VerificationAttemptService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(NOW);
    }

    @Test
    void successWithoutPreviousFailures_passesWithoutWritingAnything() {
        VerificationResult result = service.verify(USER_ID, CAMPAIGN_SCOPE, () -> true);

        assertThat(result.status()).isEqualTo(VerificationResult.Status.OK);
        verify(attemptRepository, never()).save(any());
        verify(lockoutRepository, never()).save(any());
    }

    @Test
    void failure_isCountedAndReportedAsAGenericFailure() {
        VerificationResult result = service.verify(USER_ID, CAMPAIGN_SCOPE, () -> false);

        assertThat(result.status()).isEqualTo(VerificationResult.Status.FAILED);
        ArgumentCaptor<VerificationAttempt> saved = ArgumentCaptor.forClass(VerificationAttempt.class);
        verify(attemptRepository).save(saved.capture());
        assertThat(saved.getValue().getFailedAttempts()).isEqualTo(1);
        assertThat(saved.getValue().getScope()).isEqualTo("CAMPAIGN:" + CAMPAIGN_ID);
        assertThatThrownBy(() -> result.orThrow(InvalidPasswordException::new))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void fifthConsecutiveFailure_locksTheVerificationAndRecordsTheLockout() {
        VerificationAttempt attempt = attemptWithFailures(MAX_ATTEMPTS - 1);

        VerificationResult result = service.verify(USER_ID, CAMPAIGN_SCOPE, () -> false);

        assertThat(result.status()).isEqualTo(VerificationResult.Status.LOCKED);
        assertThat(result.retryAfter()).isEqualTo(Duration.ofMinutes(LOCK_MINUTES));
        assertThat(attempt.isLockedAt(NOW)).isTrue();
        assertThat(attempt.getFailedAttempts()).isZero();

        ArgumentCaptor<VerificationLockout> lockout = ArgumentCaptor.forClass(VerificationLockout.class);
        verify(lockoutRepository).save(lockout.capture());
        assertThat(lockout.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(lockout.getValue().getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(lockout.getValue().getFailedAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(lockout.getValue().getLockedAt()).isEqualTo(NOW);
        assertThat(lockout.getValue().getLockedUntil()).isEqualTo(NOW.plus(LOCK_MINUTES, ChronoUnit.MINUTES));
    }

    @Test
    void whileLocked_theCheckIsNotEvenEvaluatedAndTheWaitTimeIsReported() {
        VerificationAttempt attempt = new VerificationAttempt(USER_ID, CAMPAIGN_SCOPE.key(), NOW);
        attempt.lockUntil(NOW.plus(5, ChronoUnit.MINUTES), NOW);
        stubAttempt(attempt);
        AtomicBoolean checked = new AtomicBoolean(false);

        VerificationResult result = service.verify(USER_ID, CAMPAIGN_SCOPE, () -> {
            checked.set(true);
            return true;
        });

        assertThat(checked).isFalse();
        assertThat(result.status()).isEqualTo(VerificationResult.Status.LOCKED);
        assertThatThrownBy(() -> result.orThrow(InvalidPasswordException::new))
                .isInstanceOf(VerificationLockedException.class)
                .hasMessage("Demasiados intentos fallidos. Podrás intentarlo de nuevo en 5 minutos.");
    }

    @Test
    void successAfterFailures_resetsTheCounter() {
        VerificationAttempt attempt = attemptWithFailures(3);

        VerificationResult result = service.verify(USER_ID, CAMPAIGN_SCOPE, () -> true);

        assertThat(result.status()).isEqualTo(VerificationResult.Status.OK);
        assertThat(attempt.getFailedAttempts()).isZero();
        verify(attemptRepository).save(attempt);
    }

    @Test
    void afterTheLockExpires_theVerificationIsCheckedAgain() {
        VerificationAttempt attempt = new VerificationAttempt(USER_ID, CAMPAIGN_SCOPE.key(), NOW);
        attempt.lockUntil(NOW.plus(LOCK_MINUTES, ChronoUnit.MINUTES), NOW);
        stubAttempt(attempt);
        VerificationAttemptService later = serviceAt(NOW.plus(LOCK_MINUTES + 1, ChronoUnit.MINUTES));

        VerificationResult result = later.verify(USER_ID, CAMPAIGN_SCOPE, () -> true);

        assertThat(result.status()).isEqualTo(VerificationResult.Status.OK);
        assertThat(attempt.getLockedUntil()).isNull();
    }

    @Test
    void accountPasswordLockout_isRecordedWithoutACampaign() {
        when(attemptRepository.findByUserIdAndScope(USER_ID, "ACCOUNT")).thenReturn(Optional.of(
                failedAttempt("ACCOUNT", MAX_ATTEMPTS - 1)));

        service.verify(USER_ID, VerificationScope.account(), () -> false);

        ArgumentCaptor<VerificationLockout> lockout = ArgumentCaptor.forClass(VerificationLockout.class);
        verify(lockoutRepository).save(lockout.capture());
        assertThat(lockout.getValue().getCampaignId()).isNull();
        assertThat(lockout.getValue().getScope()).isEqualTo("ACCOUNT");
    }

    @Test
    void limitIsConfigurable() {
        VerificationAttemptService strict = new VerificationAttemptService(
                attemptRepository, lockoutRepository, Clock.fixed(NOW, ZoneOffset.UTC), 2, LOCK_MINUTES);
        attemptWithFailures(1);

        assertThat(strict.verify(USER_ID, CAMPAIGN_SCOPE, () -> false).status())
                .isEqualTo(VerificationResult.Status.LOCKED);
    }

    @Test
    void invalidConfiguration_isRejectedAtStartup() {
        assertThatThrownBy(() -> new VerificationAttemptService(
                attemptRepository, lockoutRepository, Clock.systemUTC(), 0, LOCK_MINUTES))
                .isInstanceOf(IllegalStateException.class);
    }

    private VerificationAttemptService serviceAt(Instant now) {
        return new VerificationAttemptService(
                attemptRepository, lockoutRepository, Clock.fixed(now, ZoneOffset.UTC), MAX_ATTEMPTS, LOCK_MINUTES);
    }

    private VerificationAttempt attemptWithFailures(int failures) {
        VerificationAttempt attempt = failedAttempt(CAMPAIGN_SCOPE.key(), failures);
        stubAttempt(attempt);
        return attempt;
    }

    private VerificationAttempt failedAttempt(String scope, int failures) {
        VerificationAttempt attempt = new VerificationAttempt(USER_ID, scope, NOW);
        for (int i = 0; i < failures; i++) {
            attempt.registerFailure(NOW);
        }
        return attempt;
    }

    private void stubAttempt(VerificationAttempt attempt) {
        when(attemptRepository.findByUserIdAndScope(USER_ID, CAMPAIGN_SCOPE.key())).thenReturn(Optional.of(attempt));
    }
}
