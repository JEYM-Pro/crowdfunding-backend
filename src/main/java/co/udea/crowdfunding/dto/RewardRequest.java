package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** HU-06: una recompensa de la campaña (monto mínimo y unidades disponibles mayores a cero). */
public record RewardRequest(

        @NotBlank(message = "El nombre de la recompensa es obligatorio.")
        @Size(max = 150, message = "El nombre de la recompensa no puede superar 150 caracteres")
        String name,

        @Size(max = 500, message = "La descripción de la recompensa no puede superar 500 caracteres")
        String description,

        @NotNull(message = "El monto mínimo de la recompensa es obligatorio.")
        @Positive(message = "El monto mínimo de la recompensa debe ser mayor a cero.")
        @Digits(integer = 10, fraction = 2, message = "El monto mínimo excede el valor máximo permitido")
        BigDecimal minAmount,

        @NotNull(message = "Las unidades disponibles son obligatorias.")
        @Min(value = 1, message = "Las unidades disponibles deben ser mayores a cero.")
        Integer stock
) {
}
