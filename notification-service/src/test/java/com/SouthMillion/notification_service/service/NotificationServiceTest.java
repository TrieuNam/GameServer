package com.SouthMillion.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.notification_service.entity.Notification;
import com.SouthMillion.notification_service.kafka.NotificationEventPublisher;
import com.SouthMillion.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository      notificationRepository;
    @Mock private JavaMailSender              mailSender;
    @Mock private NotificationEventPublisher  eventPublisher;
    @Spy  private ObjectMapper                objectMapper = new ObjectMapper();

    @InjectMocks private NotificationService notificationService;

    // =========================================================
    // createNotification – IN_GAME
    // =========================================================
    @Nested
    @DisplayName("createNotification()")
    class CreateNotification {

        @Test
        @DisplayName("TC-NOTIF-001 [P] IN_GAME notification – luu va danh dau da gui")
        void createNotification_inGame_savedAndMarkedSent() {
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            Notification result = notificationService.createNotification(
                    1L, "IN_GAME", "Daily Reward", "You earned 100 gold!", Map.of());

            assertThat(result.getType()).isEqualTo("IN_GAME");
            assertThat(result.getStatus()).isEqualTo("SENT");
            then(mailSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-NOTIF-002 [P] EMAIL notification co email – gui qua mailSender")
        void createNotification_email_sendsMail() {
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            Notification result = notificationService.createNotification(
                    1L, "EMAIL", "Welcome!", "Hello player",
                    Map.of("email", "player@example.com"));

            assertThat(result.getStatus()).isEqualTo("SENT");
            then(mailSender).should().send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("TC-NOTIF-003 [P] EMAIL notification khong co email – danh dau sent khong gui mail")
        void createNotification_emailWithoutAddress_markedSentNoMail() {
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // Data without 'email' key
            Notification result = notificationService.createNotification(
                    1L, "EMAIL", "Notice", "Message", Map.of("otherKey", "value"));

            assertThat(result.getStatus()).isEqualTo("SENT");
            then(mailSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("TC-NOTIF-004 [P] PUSH notification – danh dau sent khong gui email")
        void createNotification_push_markedSentNoMail() {
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            Notification result = notificationService.createNotification(
                    1L, "PUSH", "Arena Result", "You won!", Map.of("deviceToken", "abc123"));

            assertThat(result.getStatus()).isEqualTo("SENT");
            then(mailSender).shouldHaveNoInteractions();
        }
    }

    // =========================================================
    // markAsRead
    // =========================================================
    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("TC-NOTIF-005 [P] Danh dau da doc – cap nhat trang thai READ")
        void markAsRead_found_updatesStatus() {
            Notification notif = new Notification();
            notif.setId(10L);
            notif.setPlayerId(1L);
            notif.setType("IN_GAME");
            notif.setTitle("Test");
            notif.setStatus("SENT");
            given(notificationRepository.findById(10L)).willReturn(Optional.of(notif));
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            notificationService.markAsRead(10L);

            then(notificationRepository).should().save(argThat(n ->
                    "READ".equals(n.getStatus()) && n.getReadAt() != null));
            then(eventPublisher).should().publishNotificationRead(eq(10L), eq(1L));
        }

        @Test
        @DisplayName("TC-NOTIF-006 [N] Notification khong ton tai – nem RuntimeException")
        void markAsRead_notFound_throwsException() {
            given(notificationRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Notification not found");
        }
    }

    // =========================================================
    // markAsSent / markAsFailed
    // =========================================================
    @Nested
    @DisplayName("markAsSent() / markAsFailed()")
    class MarkStatus {

        @Test
        @DisplayName("TC-NOTIF-007 [P] Danh dau da gui – cap nhat SENT va publish event")
        void markAsSent_updatesSentStatus() {
            Notification notif = new Notification();
            notif.setId(1L);
            notif.setPlayerId(1L);
            notif.setType("IN_GAME");
            notif.setTitle("Test");
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            notificationService.markAsSent(notif);

            assertThat(notif.getStatus()).isEqualTo("SENT");
            assertThat(notif.getSentAt()).isNotNull();
            then(eventPublisher).should().publishNotificationSent(eq(1L), eq(1L), anyString(), anyString());
        }

        @Test
        @DisplayName("TC-NOTIF-008 [P] Danh dau that bai – cap nhat FAILED va publish event")
        void markAsFailed_updatesFailedStatus() {
            Notification notif = new Notification();
            notif.setId(2L);
            notif.setPlayerId(2L);
            given(notificationRepository.save(any(Notification.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            notificationService.markAsFailed(notif, "SMTP error");

            assertThat(notif.getStatus()).isEqualTo("FAILED");
            assertThat(notif.getErrorMessage()).isEqualTo("SMTP error");
            then(eventPublisher).should().publishNotificationFailed(eq(2L), eq(2L), eq("SMTP error"));
        }
    }

    // =========================================================
    // getPlayerNotifications / getUnreadNotifications
    // =========================================================
    @Nested
    @DisplayName("getPlayerNotifications() / getUnreadNotifications()")
    class Queries {

        @Test
        @DisplayName("TC-NOTIF-009 [P] Lay tat ca notification cua nguoi choi")
        void getPlayerNotifications_returnsList() {
            Notification n = new Notification();
            n.setPlayerId(1L);
            n.setStatus("SENT");
            given(notificationRepository.findByPlayerIdOrderByCreatedAtDesc(1L)).willReturn(List.of(n));

            List<Notification> result = notificationService.getPlayerNotifications(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-NOTIF-010 [P] Lay notification chua doc (SENT)")
        void getUnreadNotifications_returnsSentList() {
            Notification n = new Notification();
            n.setStatus("SENT");
            given(notificationRepository.findByPlayerIdAndStatus(1L, "SENT")).willReturn(List.of(n));

            List<Notification> result = notificationService.getUnreadNotifications(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("SENT");
        }

        @Test
        @DisplayName("TC-NOTIF-011 [P] Nguoi choi chua co notification – tra ve danh sach rong")
        void getPlayerNotifications_noNotifications_returnsEmpty() {
            given(notificationRepository.findByPlayerIdOrderByCreatedAtDesc(99L)).willReturn(List.of());

            List<Notification> result = notificationService.getPlayerNotifications(99L);

            assertThat(result).isEmpty();
        }
    }
}
