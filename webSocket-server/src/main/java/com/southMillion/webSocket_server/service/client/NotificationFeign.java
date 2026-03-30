package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "notification-service")
public interface NotificationFeign {
    
    @PostMapping("/api/notification/send")
    Map<String, Object> sendNotification(@RequestBody Map<String, Object> request);
    
    @GetMapping("/api/notification/player/{playerId}")
    List<Map<String, Object>> getPlayerNotifications(@PathVariable("playerId") Long playerId);
    
    @GetMapping("/api/notification/player/{playerId}/unread")
    List<Map<String, Object>> getUnreadNotifications(@PathVariable("playerId") Long playerId);
    
    @PostMapping("/api/notification/{notificationId}/read")
    Map<String, Object> markAsRead(@PathVariable("notificationId") Long notificationId);
}
