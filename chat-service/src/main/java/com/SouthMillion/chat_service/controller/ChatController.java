package com.SouthMillion.chat_service.controller;

import com.SouthMillion.chat_service.dto.ChatDTO;
import com.SouthMillion.chat_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Chat REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<ChatDTO.Response<ChatDTO.MessageInfo>> sendMessage(
            @Valid @RequestBody ChatDTO.SendMessageRequest request) {
        log.info("REST API: Send message - channel={}, sender={}", request.getChannel(), request.getSenderId());
        return ResponseEntity.ok(chatService.sendMessage(request));
    }

    @PostMapping("/history")
    public ResponseEntity<ChatDTO.Response<List<ChatDTO.MessageInfo>>> getHistory(
            @Valid @RequestBody ChatDTO.GetHistoryRequest request) {
        log.info("REST API: Get history - channel={}", request.getChannel());
        return ResponseEntity.ok(chatService.getHistory(request));
    }

    @PostMapping("/mute")
    public ResponseEntity<ChatDTO.Response<Void>> mutePlayer(
            @Valid @RequestBody ChatDTO.MuteRequest request) {
        log.info("REST API: Mute player - roleId={}", request.getRoleId());
        return ResponseEntity.ok(chatService.mutePlayer(request));
    }

    @DeleteMapping("/unmute/{roleId}")
    public ResponseEntity<ChatDTO.Response<Void>> unmutePlayer(@PathVariable String roleId) {
        log.info("REST API: Unmute player - roleId={}", roleId);
        return ResponseEntity.ok(chatService.unmutePlayer(roleId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat Service is running!");
    }
}
