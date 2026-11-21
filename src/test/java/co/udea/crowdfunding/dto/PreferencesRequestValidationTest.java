package co.udea.crowdfunding.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-22 / HU-23: valores aceptados para el tamaño de texto y el tema. */
class PreferencesRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"small", "medium", "large", "xlarge"})
    void acceptsEveryFontSize(String fontSize) {
        assertThat(validator.validate(new PreferencesRequest(fontSize, null, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"light", "dark", "system"})
    void acceptsEveryTheme(String theme) {
        assertThat(validator.validate(new PreferencesRequest(null, theme, null))).isEmpty();
    }

    @Test
    void rejectsUnknownValues() {
        Set<ConstraintViolation<PreferencesRequest>> violations =
                validator.validate(new PreferencesRequest("huge", "sepia", "fr"));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("fontSize", "theme", "language");
    }

    @Test
    void allFieldsAreOptional() {
        assertThat(validator.validate(new PreferencesRequest(null, null, null))).isEmpty();
    }
}
