package com.SouthMillion.notification_service.controller;

import com.SouthMillion.notification_service.entity.Notification;
import com.SouthMillion.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, Object> request) {
        Long playerId = ((Number) request.get("playerId")).longValue();
        String type = (String) request.get("type");
        String title = (String) request.get("title");
        String message = (String) request.get("message");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) request.getOrDefault("data", Map.of());
        
        Notification notification = notificationService.createNotification(playerId, type, title, message, data);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "notificationId", notification.getId()
        ));
    }
    
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Notification>> getPlayerNotifications(@PathVariable Long playerId) {
        return ResponseEntity.ok(notificationService.getPlayerNotifications(playerId));
    }
    
    @GetMapping("/player/{playerId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long playerId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(playerId));
    }
    
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
