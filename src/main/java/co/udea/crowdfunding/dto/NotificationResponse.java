package co.udea.crowdfunding.dto;

import co.udea.crowdfunding.entity.Notification;

import java.time.Instant;
import java.util.UUID;

/** HU-15: notificación de la plataforma (por ejemplo, un hito de financiación alcanzado). */
public record NotificationResponse(
        UUID id,
        UUID campaignId,
        String title,
        String message,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCampaignId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
