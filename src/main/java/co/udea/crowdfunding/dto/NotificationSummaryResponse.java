package co.udea.crowdfunding.dto;

import java.util.List;

/** HU-15: bandeja de notificaciones del usuario, de la más reciente a la más antigua. */
public record NotificationSummaryResponse(
        long unreadCount,
        List<NotificationResponse> notifications) {
}
