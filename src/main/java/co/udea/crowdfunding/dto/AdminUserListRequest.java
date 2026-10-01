package co.udea.crowdfunding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserListRequest(

        @Min(value = 0, message = "La página debe ser mayor o igual a 0")
        Integer page,

        @Min(value = 1, message = "El tamaño debe ser mayor a 0")
        @Max(value = 100, message = "El tamaño no puede superar 100")
        Integer size,

        @Pattern(regexp = "^(createdAt|name|email|role|status)$", message = "Campo de ordenamiento inválido")
        String sort,

        @Size(max = 255, message = "El filtro de correo no puede superar 255 caracteres")
        String email,

        @Pattern(regexp = "^(sponsor|creator|admin)$", message = "Rol inválido")
        String role,

        @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Estado inválido")
        String status
) {
    public int getPage() {
        return page != null ? page : 0;
    }

    public int getSize() {
        return size != null ? size : 20;
    }

    public String getSort() {
        return sort != null ? sort : "createdAt,desc";
    }

    public String getEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    public String getRole() {
        return role != null ? role.trim().toLowerCase() : null;
    }

    public String getStatus() {
        return status != null ? status.trim().toUpperCase() : null;
    }
}