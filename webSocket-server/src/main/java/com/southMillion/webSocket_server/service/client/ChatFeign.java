package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for chat-service
 * Handles real-time chat operations
 */
@FeignClient(name = "chat-service", path = "/api/chat")
public interface ChatFeign {
    
    /**
     * Send chat message
     */
    @PostMapping("/send")
    Map<String, Object> sendMessage(@RequestBody Map<String, Object> request);
    
    /**
     * Get chat history
     */
    @PostMapping("/history")
    Map<String, Object> getHistory(@RequestBody Map<String, Object> request);
    
    /**
     * Mute player
     */
    @PostMapping("/mute")
    Map<String, Object> mutePlayer(@RequestBody Map<String, Object> request);
    
    /**
     * Unmute player
     */
    @DeleteMapping("/unmute/{roleId}")
    Map<String, Object> unmutePlayer(@PathVariable("roleId") String roleId);
    
    /**
     * Get player's chat info (mute status, etc)
     */
    @GetMapping("/player/{roleId}/info")
    Map<String, Object> getPlayerChatInfo(@PathVariable("roleId") String roleId);
}
