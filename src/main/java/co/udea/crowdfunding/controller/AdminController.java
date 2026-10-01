package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.AdminCreateUserRequest;
import co.udea.crowdfunding.dto.AdminUpdateUserRequest;
import co.udea.crowdfunding.dto.AdminUserActionRequest;
import co.udea.crowdfunding.dto.AdminUserDetailResponse;
import co.udea.crowdfunding.dto.AdminUserListRequest;
import co.udea.crowdfunding.dto.AdminUserListResponse;
import co.udea.crowdfunding.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Usuarios", description = "Gestión de usuarios por administradores")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios con paginación y filtros")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista paginada de usuarios"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    })
    public ResponseEntity<AdminUserListResponse> listUsers(
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1-100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo y dirección de ordenamiento (ej: createdAt,desc)") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Filtrar por correo electrónico") @RequestParam(required = false) String email,
            @Parameter(description = "Filtrar por rol (sponsor, creator, admin)") @RequestParam(required = false) String role,
            @Parameter(description = "Filtrar por estado (ACTIVE, INACTIVE)") @RequestParam(required = false) String status
    ) {
        AdminUserListRequest request = new AdminUserListRequest(page, size, sort, email, role, status);
        AdminUserListResponse response = adminUserService.listUsers(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle del usuario"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    })
    public ResponseEntity<AdminUserDetailResponse> getUser(
            @Parameter(description = "ID del usuario") @PathVariable UUID id
    ) {
        AdminUserDetailResponse response = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario (solo sponsor o creator)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol no permitido"),
            @ApiResponse(responseCode = "409", description = "El correo ya está registrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    })
    public ResponseEntity<AdminUserDetailResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminUserDetailResponse response = adminUserService.createUser(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rol no permitido"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "409", description = "El correo ya pertenece a otro usuario"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    })
    public ResponseEntity<AdminUserDetailResponse> updateUser(
            @Parameter(description = "ID del usuario") @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminUserDetailResponse response = adminUserService.updateUser(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado de un usuario (DISABLE/ENABLE)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Acción inválida o intento de auto-desactivación"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    })
    public ResponseEntity<Void> changeUserStatus(
            @Parameter(description = "ID del usuario") @PathVariable UUID id,
            @Valid @RequestBody AdminUserActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        adminUserService.changeStatus(id, request, principal.getId());
        return ResponseEntity.noContent().build();
    }
}