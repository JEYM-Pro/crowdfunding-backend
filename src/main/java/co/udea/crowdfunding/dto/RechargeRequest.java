package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RechargeRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "10000", message = "El monto mínimo es $10,000 COP")
        @DecimalMax(value = "1000000", message = "El monto máximo es $1,000,000 COP")
        BigDecimal amount
) {
}
