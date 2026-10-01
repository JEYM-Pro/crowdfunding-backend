package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminUserActionRequest(

        @NotBlank(message = "La acción es obligatoria")
        @Pattern(regexp = "^(DISABLE|ENABLE)$", message = "La acción debe ser 'DISABLE' o 'ENABLE'")
        String action
) {
    public String getAction() {
        return action != null ? action.trim().toUpperCase() : null;
    }
}