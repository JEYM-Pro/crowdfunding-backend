package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.NotificationResponse;
import co.udea.crowdfunding.dto.NotificationSummaryResponse;
import co.udea.crowdfunding.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones",
        description = "Notificaciones de la plataforma, como los hitos de financiación")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "HU-15: Consultar mis notificaciones y cuántas siguen sin leer")
    public ResponseEntity<NotificationSummaryResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(notificationService.getNotifications(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "HU-15: Marcar una notificación como leída")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        return ResponseEntity.ok(notificationService.markAsRead(principal.getId(), id));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "HU-15: Marcar todas mis notificaciones como leídas")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {

        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
