package com.SouthMillion.moderation_service.controller;

import com.SouthMillion.moderation_service.entity.Report;
import com.SouthMillion.moderation_service.entity.Violation;
import com.SouthMillion.moderation_service.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationController {
    
    private final ModerationService moderationService;
    
    @PostMapping("/filter")
    public Map<String, Object> filterMessage(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        String message = (String) request.get("message");
        return moderationService.filterMessage(userId, message);
    }

    @PostMapping("/report")
    public Report createReport(@RequestBody Map<String, Object> request) {
        String reporterId = request.get("reporterId").toString();
        String reportedUserId = request.get("reportedUserId").toString();
        String reportType = (String) request.get("reportType");
        String content = (String) request.get("content");
        String evidence = (String) request.get("evidence");

        return moderationService.createReport(reporterId, reportedUserId, reportType, content, evidence);
    }
    
    @PutMapping("/report/{reportId}/handle")
    public Map<String, Object> handleReport(
        @PathVariable Long reportId,
        @RequestBody Map<String, Object> request
    ) {
        String gmId = request.get("gmId").toString();
        String resolution = (String) request.get("resolution");
        String action = (String) request.get("action");

        moderationService.handleReport(reportId, gmId, resolution, action);
        return Map.of("success", true, "message", "Report handled");
    }
    
    @GetMapping("/reports/pending")
    public List<Report> getPendingReports() {
        return moderationService.getPendingReports();
    }
    
    @GetMapping("/violations/{userId}")
    public List<Violation> getUserViolations(@PathVariable String userId) {
        return moderationService.getUserViolations(userId);
    }
    
    @GetMapping("/check/{userId}/muted")
    public Map<String, Object> checkMuted(@PathVariable String userId) {
        boolean muted = moderationService.isUserMuted(userId);
        return Map.of("userId", userId, "muted", muted);
    }
    
    @GetMapping("/check/{userId}/banned")
    public Map<String, Object> checkBanned(@PathVariable String userId) {
        boolean banned = moderationService.isUserBanned(userId);
        return Map.of("userId", userId, "banned", banned);
    }
    
    @PostMapping("/mute/{userId}")
    public Map<String, Object> muteUser(@PathVariable String userId, @RequestBody Map<String, Object> request) {
        int hours = (int) request.getOrDefault("hours", 1);
        String reason = (String) request.getOrDefault("reason", "Manual mute");
        moderationService.muteUser(userId, hours, reason);
        return Map.of("success", true, "message", "User muted");
    }
    
    @PostMapping("/ban/{userId}")
    public Map<String, Object> banUser(@PathVariable String userId, @RequestBody Map<String, Object> request) {
        int hours = (int) request.getOrDefault("hours", 24);
        String reason = (String) request.getOrDefault("reason", "Manual ban");
        moderationService.banUser(userId, hours, reason);
        return Map.of("success", true, "message", "User banned");
    }
}
