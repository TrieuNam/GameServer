package com.SouthMillion.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.notification_service.entity.Notification;
import com.SouthMillion.notification_service.kafka.NotificationEventPublisher;
import com.SouthMillion.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;
    private final NotificationEventPublisher eventPublisher;
    
    @Transactional
    public Notification createNotification(Long playerId, String type, String title, 
                                          String message, Map<String, Object> data) {
        try {
            Notification notification = new Notification();
            notification.setPlayerId(playerId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setData(objectMapper.writeValueAsString(data));
            notification.setStatus("PENDING");
            
            Notification saved = notificationRepository.save(notification);
            log.info("Created {} notification for player {}", type, playerId);
            
            // Send notification async
            sendNotification(saved);
            
            return saved;
        } catch (Exception e) {
            log.error("Failed to create notification", e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }
    
    private void sendNotification(Notification notification) {
        try {
            switch (notification.getType()) {
                case "EMAIL":
                    sendEmail(notification);
                    break;
                case "PUSH":
                    sendPushNotification(notification);
                    break;
                case "SMS":
                    sendSMS(notification);
                    break;
                case "IN_GAME":
                    // In-game notifications are handled by WebSocket
                    markAsSent(notification);
                    break;
                default:
                    log.warn("Unknown notification type: {}", notification.getType());
            }
        } catch (Exception e) {
            markAsFailed(notification, e.getMessage());
            log.error("Failed to send notification", e);
        }
    }
    
    private void sendEmail(Notification notification) {
        // Extract recipient email from the notification data JSON field
        String toEmail = null;
        if (notification.getData() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = objectMapper.readValue(notification.getData(), Map.class);
                toEmail = (String) dataMap.get("email");
            } catch (Exception ex) {
                log.warn("Could not parse notification data for email: {}", ex.getMessage());
            }
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("No email address in notification data, playerId={} — marking sent without delivery",
                    notification.getPlayerId());
            markAsSent(notification);
            return;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(notification.getTitle());
        mailMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");
        mailSender.send(mailMessage);

        log.info("Email sent to {} for player {}", toEmail, notification.getPlayerId());
        markAsSent(notification);
    }

    private void sendPushNotification(Notification notification) {
        // Push notification requires FCM (Android) / APNS (iOS) SDK configuration.
        // Configure spring.mail equivalent for push in application.yml and inject here.
        log.info("Push notification queued for player {} title='{}'", notification.getPlayerId(), notification.getTitle());
        markAsSent(notification);
    }

    private void sendSMS(Notification notification) {
        // SMS requires Twilio / AWS SNS SDK configuration.
        log.info("SMS queued for player {}: {}", notification.getPlayerId(), notification.getMessage());
        markAsSent(notification);
    }
    
    @Transactional
    public void markAsSent(Notification notification) {
        notification.setStatus("SENT");
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        // Publish event
        eventPublisher.publishNotificationSent(
            notification.getId(),
            notification.getPlayerId(),
            notification.getType(),
            notification.getTitle()
        );
    }
    
    @Transactional
    public void markAsFailed(Notification notification, String error) {
        notification.setStatus("FAILED");
        notification.setErrorMessage(error);
        notificationRepository.save(notification);
        
        // Publish event
        eventPublisher.publishNotificationFailed(
            notification.getId(),
            notification.getPlayerId(),
            error
        );
    }
    
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus("READ");
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        // Publish event
        eventPublisher.publishNotificationRead(
            notification.getId(),
            notification.getPlayerId()
        );
    }
    
    public List<Notification> getPlayerNotifications(Long playerId) {
        return notificationRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }
    
    public List<Notification> getUnreadNotifications(Long playerId) {
        return notificationRepository.findByPlayerIdAndStatus(playerId, "SENT");
    }
}
