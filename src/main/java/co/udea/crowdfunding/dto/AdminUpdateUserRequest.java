package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserRequest(

        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String email,

        @Pattern(regexp = "^(sponsor|creator)$", message = "El rol debe ser 'sponsor' o 'creator'")
        String role
) {
    public String getEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    public String getRole() {
        return role != null ? role.trim().toLowerCase() : null;
    }

    public String getName() {
        return name != null ? name.trim() : null;
    }

    public boolean hasName() {
        return name != null && !name.trim().isEmpty();
    }

    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    public boolean hasRole() {
        return role != null && !role.trim().isEmpty();
    }
}