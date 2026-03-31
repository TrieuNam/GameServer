package com.SouthMillion.gm.controller;

import com.SouthMillion.gm.dto.*;
import com.SouthMillion.gm.entity.GMActionLog;
import com.SouthMillion.gm.service.GMService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Game Master Tool REST API
 */
@RestController
@RequestMapping("/api/gm")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GMController {

    private final GMService gmService;

    // ==================== Item Management ====================
    
    @PostMapping("/item/give")
    public ResponseEntity<?> giveItem(
            @Valid @RequestBody GiveItemRequest request,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.giveItems(gmId, gmUsername, request, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/item/remove")
    public ResponseEntity<?> removeItem(
            @Valid @RequestBody GiveItemRequest request,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.removeItems(gmId, gmUsername, request, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Currency Management ====================
    
    @PostMapping("/currency/add")
    public ResponseEntity<?> addCurrency(
            @Valid @RequestBody CurrencyOperationRequest request,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.addCurrency(gmId, gmUsername, request, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/currency/deduct")
    public ResponseEntity<?> deductCurrency(
            @Valid @RequestBody CurrencyOperationRequest request,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.deductCurrency(gmId, gmUsername, request, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Player Management ====================
    
    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getPlayerDetails(@PathVariable String playerId) {
        try {
            Map<String, Object> details = gmService.getPlayerDetails(playerId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserInfo(@PathVariable Long userId) {
        try {
            Map<String, Object> userInfo = gmService.getUserInfo(userId);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable Long userId) {
        try {
            Map<String, Object> roles = gmService.getUserRoles(userId);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== VIP Management ====================
    
    @PostMapping("/vip/update")
    public ResponseEntity<?> updateVipLevel(
            @Valid @RequestBody UpdateVipRequest request,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.updateVipLevel(gmId, gmUsername, request, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== User Ban/Unban ====================
    
    @PostMapping("/user/{userId}/ban")
    public ResponseEntity<?> banUser(
            @PathVariable Long userId,
            @RequestParam String reason,
            @RequestParam(required = false) Integer durationDays,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.banUser(gmId, gmUsername, userId, reason, durationDays, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}/unban")
    public ResponseEntity<?> unbanUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            Map<String, Object> result = gmService.unbanUser(gmId, gmUsername, userId, reason, ipAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Broadcast ====================
    
    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcastMessage(
            @Valid @RequestBody BroadcastRequest request,
            @RequestHeader(value = "GM-Id", defaultValue = "1") Long gmId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String gmUsername = authentication != null ? authentication.getName() : "gm";
            String ipAddress = httpRequest.getRemoteAddr();
            String result = gmService.broadcastMessage(gmId, gmUsername, request, ipAddress);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Logs ====================
    
    @GetMapping("/logs/gm/{gmId}")
    public ResponseEntity<Page<GMActionLog>> getGMLogs(
            @PathVariable Long gmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<GMActionLog> logs = gmService.getGMLogs(gmId, PageRequest.of(page, size));
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/player/{playerId}")
    public ResponseEntity<Page<GMActionLog>> getPlayerLogs(
            @PathVariable String playerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<GMActionLog> logs = gmService.getPlayerActionLogs(playerId, PageRequest.of(page, size));
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/recent")
    public ResponseEntity<List<GMActionLog>> getRecentLogs() {
        List<GMActionLog> logs = gmService.getRecentLogs();
        return ResponseEntity.ok(logs);
    }
}
