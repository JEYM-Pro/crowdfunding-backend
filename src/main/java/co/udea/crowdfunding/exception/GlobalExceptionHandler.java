package co.udea.crowdfunding.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Datos inválidos", details));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(InvalidCampaignException.class)
    public ResponseEntity<ApiError> handleInvalidCampaign(InvalidCampaignException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<ApiError> handleCampaignNotFound(CampaignNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(CampaignAccessDeniedException.class)
    public ResponseEntity<ApiError> handleCampaignAccessDenied(CampaignAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(CampaignNotEditableException.class)
    public ResponseEntity<ApiError> handleCampaignNotEditable(CampaignNotEditableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(InvalidContributionException.class)
    public ResponseEntity<ApiError> handleInvalidContribution(InvalidContributionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(ContributionProcessingException.class)
    public ResponseEntity<ApiError> handleContributionProcessing(ContributionProcessingException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("El formato de los datos enviados no es válido", Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        HttpStatus status = message.contains("inválidas") || message.contains("no encontrado")
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiError(message, Map.of()));
    }
}