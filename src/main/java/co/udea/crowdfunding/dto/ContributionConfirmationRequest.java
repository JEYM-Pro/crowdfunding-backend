package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** HU-11/HU-12: aporte a una campaña con recompensa opcional. */
public record ContributionConfirmationRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite máximo 2 decimales")
        BigDecimal amount,

        /** Opcional: debe ser una de las recompensas de la campaña. */
        UUID rewardId,

        /**
         * HU-S06: contraseña de la cuenta. Se valida en el servicio (y no aquí) para responder
         * siempre el mismo error genérico, tanto si falta como si es incorrecta.
         */
        String password
) {

    /** La contraseña no debe aparecer en registros ni en trazas. */
    @Override
    public String toString() {
        return "ContributionConfirmationRequest[amount=" + amount + ", rewardId=" + rewardId + ", password=****]";
    }
}
