package co.udea.crowdfunding.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerContributionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidContribution_isBadRequest() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidContribution(new InvalidContributionException("Saldo insuficiente para realizar el aporte."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Saldo insuficiente para realizar el aporte.");
    }

    @Test
    void contributionProcessingFailure_isInternalServerError() {
        ResponseEntity<ApiError> response =
                handler.handleContributionProcessing(new ContributionProcessingException("El servicio de correo no está configurado."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("El servicio de correo no está configurado.");
    }
}
