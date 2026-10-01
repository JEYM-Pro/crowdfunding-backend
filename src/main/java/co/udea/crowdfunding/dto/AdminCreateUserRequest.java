package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 255, message = "El correo no puede superar 255 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*\\p{Lu})(?=.*\\d)(?=.*[^\\p{L}\\p{N}\\s]).{8,16}$",
                message = "La contraseña debe tener entre 8 y 16 caracteres, con al menos una mayúscula, un número y un carácter especial")
        String password,

        @NotNull(message = "El rol es obligatorio")
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
}