package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Pattern;

/** HU-22 / HU-23: cada campo es opcional; el que llegue vacío conserva su valor actual. */
public record PreferencesRequest(
        @Pattern(regexp = "^(small|medium|large)$",
                message = "El tamaño de texto debe ser 'small', 'medium' o 'large'")
        String fontSize,

        @Pattern(regexp = "^(light|dark)$",
                message = "El tema debe ser 'light' o 'dark'")
        String theme,

        @Pattern(regexp = "^(es|en)$",
                message = "El idioma debe ser 'es' o 'en'")
        String language
) {
}
