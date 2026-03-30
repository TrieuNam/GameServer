package com.SouthMillion.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String NOTIFICATION_SENT_TOPIC = "notification.sent";
    private static final String NOTIFICATION_READ_TOPIC = "notification.read";
    private static final String NOTIFICATION_FAILED_TOPIC = "notification.failed";
    
    /**
     * Publish notification sent event
     */
    public void publishNotificationSent(Long notificationId, Long playerId, String type, String title) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("notificationId", notificationId);
            event.put("playerId", playerId);
            event.put("type", type);
            event.put("title", title);
            event.put("timestamp", System.currentTimeMillis());
            event.put("eventType", "NOTIFICATION_SENT");
            
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(NOTIFICATION_SENT_TOPIC, playerId.toString(), message);
            
            log.debug("Published notification sent event: notificationId={}, playerId={}", notificationId, playerId);
            
        } catch (Exception e) {
            log.error("Failed to publish notification sent event", e);
        }
    }
    
    /**
     * Publish notification read event
     */
    public void publishNotificationRead(Long notificationId, Long playerId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("notificationId", notificationId);
            event.put("playerId", playerId);
            event.put("timestamp", System.currentTimeMillis());
            event.put("eventType", "NOTIFICATION_READ");
            
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(NOTIFICATION_READ_TOPIC, playerId.toString(), message);
            
            log.debug("Published notification read event: notificationId={}, playerId={}", notificationId, playerId);
            
        } catch (Exception e) {
            log.error("Failed to publish notification read event", e);
        }
    }
    
    /**
     * Publish notification failed event
     */
    public void publishNotificationFailed(Long notificationId, Long playerId, String errorMessage) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("notificationId", notificationId);
            event.put("playerId", playerId);
            event.put("errorMessage", errorMessage);
            event.put("timestamp", System.currentTimeMillis());
            event.put("eventType", "NOTIFICATION_FAILED");
            
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(NOTIFICATION_FAILED_TOPIC, playerId.toString(), message);
            
            log.debug("Published notification failed event: notificationId={}, playerId={}", notificationId, playerId);
            
        } catch (Exception e) {
            log.error("Failed to publish notification failed event", e);
        }
    }
}
