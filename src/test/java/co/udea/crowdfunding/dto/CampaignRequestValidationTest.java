package co.udea.crowdfunding.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final Instant DEADLINE = Instant.parse("2026-11-09T12:00:00Z");
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    // ---------------------------- HU-06: CreateCampaignRequest ----------------------------

    @Test
    void create_withValidData_hasNoViolations() {
        CreateCampaignRequest request = new CreateCampaignRequest("Título", "Descripción",
                new BigDecimal("1000000"), DEADLINE, CATEGORY_ID, List.of("https://img.test/1.png"), null, "123456");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void create_withZeroGoal_isRejectedWithBusinessMessage() {
        CreateCampaignRequest request = createWithGoal(BigDecimal.ZERO);

        assertThat(messages(validator.validate(request)))
                .containsExactly("La meta de financiación debe ser mayor a cero.");
    }

    @Test
    void create_withNegativeGoal_isRejectedWithBusinessMessage() {
        CreateCampaignRequest request = createWithGoal(new BigDecimal("-500"));

        assertThat(messages(validator.validate(request)))
                .containsExactly("La meta de financiación debe ser mayor a cero.");
    }

    @Test
    void create_withGoalExceedingColumnPrecision_isRejected() {
        CreateCampaignRequest request = createWithGoal(new BigDecimal("12345678901"));

        assertThat(messages(validator.validate(request)))
                .containsExactly("La meta de financiación excede el valor máximo permitido");
    }

    @Test
    void create_withAllFieldsMissing_reportsEveryRequiredField() {
        CreateCampaignRequest request = new CreateCampaignRequest(" ", "", null, null, null, null, null, null);

        assertThat(messages(validator.validate(request))).containsExactlyInAnyOrder(
                "El título es obligatorio.",
                "La descripción es obligatoria.",
                "La meta de financiación es obligatoria.",
                "La fecha límite es obligatoria.",
                "La contraseña de campaña es obligatoria.");
    }

    // ---------------- HU-S02: contraseña de campaña (PIN de 6 dígitos)

    @Test
    void create_withSixDigitPin_isValid() {
        assertThat(validator.validate(createWithPin("123456"))).isEmpty();
        assertThat(validator.validate(createWithPin("000000"))).isEmpty();
    }

    @Test
    void create_withBlankPin_isRejectedAsRequired() {
        assertThat(messages(validator.validate(createWithPin(" "))))
                .contains("La contraseña de campaña es obligatoria.");
    }

    @Test
    void create_withWrongPinFormat_isRejectedShowingTheRequiredFormat() {
        for (String invalid : List.of("12345", "1234567", "12345a", "abcdef", "12 456", "12.456")) {
            assertThat(messages(validator.validate(createWithPin(invalid))))
                    .as("PIN %s", invalid)
                    .contains("La contraseña de campaña debe ser un PIN de exactamente 6 dígitos numéricos.");
        }
    }

    @Test
    void requests_neverPrintThePinInTheirStringRepresentation() {
        assertThat(createWithPin("987654").toString()).doesNotContain("987654");
        assertThat(new UpdateCampaignRequest("T", "D", null, null, null, null, null, "987654").toString())
                .doesNotContain("987654");
    }

    @Test
    void create_withMoreThanFourImages_isRejected() {
        List<String> images = List.of("https://a.test/1.png", "https://a.test/2.png", "https://a.test/3.png",
                "https://a.test/4.png", "https://a.test/5.png");
        CreateCampaignRequest request = new CreateCampaignRequest("Título", "Descripción",
                BigDecimal.TEN, DEADLINE, CATEGORY_ID, images, null, "123456");

        assertThat(messages(validator.validate(request)))
                .containsExactly("Una campaña puede tener máximo 4 imágenes");
    }

    @Test
    void create_withNonHttpImageUrl_isRejected() {
        CreateCampaignRequest request = new CreateCampaignRequest("Título", "Descripción",
                BigDecimal.TEN, DEADLINE, CATEGORY_ID, List.of("ftp://servidor/imagen.png"), null, "123456");

        assertThat(messages(validator.validate(request)))
                .containsExactly("Cada imagen debe ser una URL válida (http, https o data)");
    }

    @Test
    void create_withUploadedImageDataUrlWithinLimit_isAccepted() {
        String prefix = "data:image/png;base64,";
        String image = prefix + "A".repeat(2_800_000 - prefix.length());
        CreateCampaignRequest request = new CreateCampaignRequest("Título", "Descripción",
                BigDecimal.TEN, DEADLINE, CATEGORY_ID, List.of(image), null, "123456");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void create_withUploadedImageDataUrlOverLimit_isRejected() {
        String prefix = "data:image/png;base64,";
        String image = prefix + "A".repeat(2_800_001 - prefix.length());
        CreateCampaignRequest request = new CreateCampaignRequest("Título", "Descripción",
                BigDecimal.TEN, DEADLINE, CATEGORY_ID, List.of(image), null, "123456");

        assertThat(messages(validator.validate(request)))
                .containsExactly("La imagen no puede superar el tamaño máximo permitido");
    }

    @Test
    void create_withTooLongTitle_isRejected() {
        CreateCampaignRequest request = new CreateCampaignRequest("x".repeat(256), "Descripción",
                BigDecimal.TEN, DEADLINE, CATEGORY_ID, null, null, "123456");

        assertThat(messages(validator.validate(request)))
                .containsExactly("El título no puede superar 255 caracteres");
    }

    // ---------------------------- HU-07: UpdateCampaignRequest ----------------------------

    @Test
    void update_withOnlyRequiredFields_hasNoViolations() {
        UpdateCampaignRequest request = new UpdateCampaignRequest("Título", "Descripción", null, null, null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void update_withBlankTitleAndDescription_isRejected() {
        UpdateCampaignRequest request = new UpdateCampaignRequest("", "  ", null, null, null, null, null, null);

        assertThat(messages(validator.validate(request))).containsExactlyInAnyOrder(
                "El título es obligatorio.",
                "La descripción es obligatoria.");
    }

    @Test
    void update_withNonPositiveGoal_isRejected() {
        UpdateCampaignRequest request = new UpdateCampaignRequest("Título", "Descripción", null,
                BigDecimal.ZERO, null, null, null, null);

        assertThat(messages(validator.validate(request)))
                .containsExactly("La meta de financiación debe ser mayor a cero.");
    }

    @Test
    void update_withInvalidImageUrl_isRejected() {
        UpdateCampaignRequest request = new UpdateCampaignRequest("Título", "Descripción",
                List.of("no-es-una-url"), null, null, null, null, null);

        assertThat(messages(validator.validate(request)))
                .containsExactly("Cada imagen debe ser una URL válida (http, https o data)");
    }

    // ---------------------------- Recompensas (HU-06) ----------------------------

    @Test
    void create_withValidRewards_hasNoViolations() {
        CreateCampaignRequest request = createWithRewards(
                List.of(new RewardRequest("Taza", "Pieza única", new BigDecimal("50000"), 12)));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void create_withMoreThanSixRewards_isRejected() {
        RewardRequest reward = new RewardRequest("R", null, BigDecimal.TEN, 1);
        CreateCampaignRequest request = createWithRewards(List.of(reward, reward, reward, reward, reward, reward, reward));

        assertThat(messages(validator.validate(request)))
                .containsExactly("Una campaña puede tener máximo 6 recompensas");
    }

    @Test
    void create_withZeroMinAmountOrStock_isRejected() {
        CreateCampaignRequest request = createWithRewards(
                List.of(new RewardRequest("Taza", null, BigDecimal.ZERO, 0)));

        assertThat(messages(validator.validate(request))).containsExactlyInAnyOrder(
                "El monto mínimo de la recompensa debe ser mayor a cero.",
                "Las unidades disponibles deben ser mayores a cero.");
    }

    @Test
    void create_withIncompleteReward_reportsMissingFields() {
        CreateCampaignRequest request = createWithRewards(List.of(new RewardRequest(" ", null, null, null)));

        assertThat(messages(validator.validate(request))).containsExactlyInAnyOrder(
                "El nombre de la recompensa es obligatorio.",
                "El monto mínimo de la recompensa es obligatorio.",
                "Las unidades disponibles son obligatorias.");
    }

    // ---------------------------------- Helpers ----------------------------------

    private CreateCampaignRequest createWithGoal(BigDecimal goal) {
        return new CreateCampaignRequest("Título", "Descripción", goal, DEADLINE, CATEGORY_ID, null, null, "123456");
    }

    private CreateCampaignRequest createWithRewards(List<RewardRequest> rewards) {
        return new CreateCampaignRequest("Título", "Descripción", BigDecimal.TEN, DEADLINE, CATEGORY_ID, null, rewards, "123456");
    }

    private CreateCampaignRequest createWithPin(String pin) {
        return new CreateCampaignRequest("Título", "Descripción", BigDecimal.TEN, DEADLINE, CATEGORY_ID, null, null, pin);
    }

    private <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
