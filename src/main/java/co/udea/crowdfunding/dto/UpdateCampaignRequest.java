package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * HU-07: edición de una campaña activa.
 * {@code goal}, {@code deadline} e {@code images} son opcionales: si vienen nulos se conserva el valor actual.
 */
public record UpdateCampaignRequest(

        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 255, message = "El título no puede superar 255 caracteres")
        String title,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
        String description,

        @Size(max = 4, message = "Una campaña puede tener máximo 4 imágenes")
        List<@NotBlank(message = "La URL de la imagen no puede estar vacía")
             @Size(max = 2000000, message = "La URL de la imagen no puede superar 2 000 000 caracteres")
             @Pattern(regexp = "^(https?://|data:).+", message = "Cada imagen debe ser una URL válida (http, https o data)")
             String> images,

        @Positive(message = "La meta de financiación debe ser mayor a cero.")
        @Digits(integer = 10, fraction = 2, message = "La meta de financiación excede el valor máximo permitido")
        BigDecimal goal,

        Instant deadline,

        /** Opcional: si viene, cambia la categoría de la campaña. */
        UUID categoryId,

        /** Las recompensas no se pueden modificar: si llegan, el servicio rechaza la edición (409). */
        List<RewardRequest> rewards
) {
}
