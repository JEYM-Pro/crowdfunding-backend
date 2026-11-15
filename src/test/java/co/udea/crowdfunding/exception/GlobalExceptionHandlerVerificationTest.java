package co.udea.crowdfunding.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-S05, HU-S07 y HU-15: respuestas HTTP de las verificaciones y de las notificaciones. */
class GlobalExceptionHandlerVerificationTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void verificationLocked_isTooManyRequestsWithTheWaitTime() {
        ResponseEntity<ApiError> response =
                handler.handleVerificationLocked(new VerificationLockedException(Duration.ofSeconds(90)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("90");
        assertThat(response.getBody().message())
                .isEqualTo("Demasiados intentos fallidos. Podrás intentarlo de nuevo en 2 minutos.");
        assertThat(response.getBody().details()).containsEntry("retryAfterSeconds", "90");
    }

    @Test
    void verificationLocked_withLessThanAMinuteLeft_saysOneMinute() {
        ResponseEntity<ApiError> response =
                handler.handleVerificationLocked(new VerificationLockedException(Duration.ofSeconds(20)));

        assertThat(response.getBody().message())
                .isEqualTo("Demasiados intentos fallidos. Podrás intentarlo de nuevo en 1 minuto.");
    }

    @Test
    void invalidCloseCredentials_isBadRequestWithoutSayingWhichOneFailed() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidCloseCredentials(new InvalidCloseCredentialsException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message())
                .doesNotContainIgnoringCase("PIN")
                .doesNotContainIgnoringCase("contraseña");
    }

    @Test
    void notificationNotFound_isNotFound() {
        ResponseEntity<ApiError> response =
                handler.handleNotificationNotFound(new NotificationNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
