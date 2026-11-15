package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.NotificationResponse;
import co.udea.crowdfunding.dto.NotificationSummaryResponse;
import co.udea.crowdfunding.entity.Notification;
import co.udea.crowdfunding.exception.NotificationNotFoundException;
import co.udea.crowdfunding.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** HU-15: consulta y lectura de las notificaciones que recibe cada usuario dentro de la plataforma. */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationSummaryResponse getNotifications(UUID userId) {
        List<NotificationResponse> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(NotificationResponse::from)
                        .toList();

        return new NotificationSummaryResponse(
                notificationRepository.countByUserIdAndReadFalse(userId),
                notifications);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        // Una notificación ajena se trata como inexistente para no revelar que existe.
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(NotificationNotFoundException::new);

        notification.markAsRead();
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .toList();

        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
    }
}
