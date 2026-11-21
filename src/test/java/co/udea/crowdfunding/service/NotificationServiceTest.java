package co.udea.crowdfunding.service;

import co.udea.crowdfunding.dto.NotificationResponse;
import co.udea.crowdfunding.dto.NotificationSummaryResponse;
import co.udea.crowdfunding.entity.Notification;
import co.udea.crowdfunding.exception.NotificationNotFoundException;
import co.udea.crowdfunding.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-15: bandeja de notificaciones dentro de la plataforma. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository);
    }

    @Test
    void getNotifications_returnsTheInboxAndTheUnreadCount() {
        Notification unread = notification(USER_ID, "Meta de 50% alcanzada");
        Notification read = notification(USER_ID, "Meta de 25% alcanzada");
        read.markAsRead();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(unread, read));
        when(notificationRepository.countByUserIdAndReadFalse(USER_ID)).thenReturn(1L);

        NotificationSummaryResponse summary = service.getNotifications(USER_ID);

        assertThat(summary.unreadCount()).isEqualTo(1);
        assertThat(summary.notifications())
                .extracting(NotificationResponse::title, NotificationResponse::read)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Meta de 50% alcanzada", false),
                        org.assertj.core.groups.Tuple.tuple("Meta de 25% alcanzada", true));
    }

    @Test
    void markAsRead_marksTheOwnNotification() {
        Notification notification = notification(USER_ID, "Meta de 75% alcanzada");
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = service.markAsRead(USER_ID, notification.getId());

        assertThat(response.read()).isTrue();
        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markAsRead_rejectsAnotherUsersNotificationAsNotFound() {
        Notification notification = notification(OTHER_USER_ID, "Meta de 100% alcanzada");
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markAsRead(USER_ID, notification.getId()))
                .isInstanceOf(NotificationNotFoundException.class);
        assertThat(notification.isRead()).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_rejectsAMissingNotification() {
        UUID missingId = UUID.randomUUID();
        when(notificationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(USER_ID, missingId))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markAllAsRead_marksEveryUnreadNotification() {
        Notification first = notification(USER_ID, "Meta de 25% alcanzada");
        Notification second = notification(USER_ID, "Meta de 50% alcanzada");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(first, second));

        service.markAllAsRead(USER_ID);

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(notificationRepository).saveAll(List.of(first, second));
    }

    private Notification notification(UUID userId, String title) {
        Notification notification = new Notification(userId, CAMPAIGN_ID, title, "Mensaje", NOW);
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        return notification;
    }
}
