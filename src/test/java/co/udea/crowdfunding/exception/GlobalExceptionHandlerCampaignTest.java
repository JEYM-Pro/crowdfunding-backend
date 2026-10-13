package co.udea.crowdfunding.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerCampaignTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidCampaign_isBadRequest() {
        ResponseEntity<ApiError> response =
                handler.handleInvalidCampaign(new InvalidCampaignException("La fecha límite debe ser posterior a hoy."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("La fecha límite debe ser posterior a hoy.");
    }

    @Test
    void campaignNotFound_isNotFound() {
        ResponseEntity<ApiError> response = handler.handleCampaignNotFound(new CampaignNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Campaña no encontrada.");
    }

    @Test
    void campaignAccessDenied_isForbidden() {
        ResponseEntity<ApiError> response =
                handler.handleCampaignAccessDenied(new CampaignAccessDeniedException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("No tienes permiso para editar esta campaña.");
    }

    @Test
    void campaignNotEditable_isConflict() {
        ResponseEntity<ApiError> response =
                handler.handleCampaignNotEditable(new CampaignNotEditableException("Solo se pueden editar campañas activas."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Solo se pueden editar campañas activas.");
    }

    @Test
    void unreadableBody_isBadRequest() {
        ResponseEntity<ApiError> response =
                handler.handleUnreadableBody(mock(HttpMessageNotReadableException.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("El formato de los datos enviados no es válido");
    }
}
