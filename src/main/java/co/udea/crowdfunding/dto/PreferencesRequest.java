package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Pattern;

/**
 * HU-22 / HU-23: cada campo es opcional; el que llegue vacío conserva su valor actual.
 * El frontend usa medium = Normal, large = Grande y xlarge = Extra grande; small se mantiene por compatibilidad.
 * El tema "system" sigue el modo claro u oscuro del dispositivo.
 */
public record PreferencesRequest(
        @Pattern(regexp = "^(small|medium|large|xlarge)$",
                message = "El tamaño de texto debe ser 'small', 'medium', 'large' o 'xlarge'")
        String fontSize,

        @Pattern(regexp = "^(light|dark|system)$",
                message = "El tema debe ser 'light', 'dark' o 'system'")
        String theme,

        @Pattern(regexp = "^(es|en)$",
                message = "El idioma debe ser 'es' o 'en'")
        String language
) {
}
