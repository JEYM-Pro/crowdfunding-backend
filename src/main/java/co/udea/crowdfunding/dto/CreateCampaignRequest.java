package co.udea.crowdfunding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** HU-06: datos para publicar una campaña. */
public record CreateCampaignRequest(

        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 255, message = "El título no puede superar 255 caracteres")
        String title,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 5000, message = "La descripción no puede superar 5000 caracteres")
        String description,

        @NotNull(message = "La meta de financiación es obligatoria.")
        @Positive(message = "La meta de financiación debe ser mayor a cero.")
        @Digits(integer = 10, fraction = 2, message = "La meta de financiación excede el valor máximo permitido")
        BigDecimal goal,

        @NotNull(message = "La fecha límite es obligatoria.")
        Instant deadline,

        @NotNull(message = "Selecciona una categoría.")
        UUID categoryId,

        @Size(max = 4, message = "Una campaña puede tener máximo 4 imágenes")
        List<@NotBlank(message = "La URL de la imagen no puede estar vacía")
             @Size(max = 500, message = "La URL de la imagen no puede superar 500 caracteres")
             @Pattern(regexp = "^https?://.+", message = "Cada imagen debe ser una URL válida (http o https)")
             String> images,

        @Size(max = 6, message = "Una campaña puede tener máximo 6 recompensas")
        List<@Valid RewardRequest> rewards
) {
}
