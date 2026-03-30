package com.SouthMillion.anti_cheat_service.controller;

import com.SouthMillion.anti_cheat_service.entity.CheatReport;
import com.SouthMillion.anti_cheat_service.entity.SuspiciousActivity;
import com.SouthMillion.anti_cheat_service.service.AntiCheatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anticheat")
@RequiredArgsConstructor
@Slf4j
public class AntiCheatController {
    
    private final AntiCheatService antiCheatService;
    
    /**
     * Report player movement for analysis
     */
    @PostMapping("/report/movement")
    public ResponseEntity<Map<String, String>> reportMovement(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        double x = Double.parseDouble(request.get("x").toString());
        double y = Double.parseDouble(request.get("y").toString());
        double z = Double.parseDouble(request.get("z").toString());
        double speed = Double.parseDouble(request.get("speed").toString());

        antiCheatService.reportMovement(userId, x, y, z, speed);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }
    
    /**
     * Report damage dealt for analysis
     */
    @PostMapping("/report/damage")
    public ResponseEntity<Map<String, String>> reportDamage(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        String targetId = request.get("targetId").toString();
        double damage = Double.parseDouble(request.get("damage").toString());
        double expectedDamage = Double.parseDouble(request.get("expectedDamage").toString());

        antiCheatService.reportDamage(userId, targetId, damage, expectedDamage);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }
    
    /**
     * Report resource gain for analysis
     */
    @PostMapping("/report/resource")
    public ResponseEntity<Map<String, String>> reportResourceGain(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        String resourceType = request.get("resourceType").toString();
        long amount = Long.parseLong(request.get("amount").toString());
        long expectedAmount = Long.parseLong(request.get("expectedAmount").toString());

        antiCheatService.reportResourceGain(userId, resourceType, amount, expectedAmount);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }
    
    /**
     * Get user's cheat history
     */
    @GetMapping("/reports/user/{userId}")
    public ResponseEntity<List<CheatReport>> getUserCheatHistory(@PathVariable String userId) {
        List<CheatReport> reports = antiCheatService.getUserCheatHistory(userId);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get pending cheat reports (GM)
     */
    @GetMapping("/reports/pending")
    public ResponseEntity<List<CheatReport>> getPendingReports() {
        List<CheatReport> reports = antiCheatService.getPendingReports();
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Get high severity reports (GM)
     */
    @GetMapping("/reports/high-severity")
    public ResponseEntity<List<CheatReport>> getHighSeverityReports(
            @RequestParam(defaultValue = "3") int minSeverity) {
        
        List<CheatReport> reports = antiCheatService.getHighSeverityReports(minSeverity);
        return ResponseEntity.ok(reports);
    }
    
    /**
     * Review cheat report (GM)
     */
    @PutMapping("/reports/{reportId}/review")
    public ResponseEntity<CheatReport> reviewCheatReport(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> request) {
        
        Long gmId = Long.parseLong(request.get("gmId").toString());
        String status = request.get("status").toString();
        String action = request.get("action").toString();
        String notes = request.getOrDefault("notes", "").toString();
        
        CheatReport report = antiCheatService.reviewCheatReport(reportId, gmId, status, action, notes);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get suspicious activities for user
     */
    @GetMapping("/suspicious/user/{userId}")
    public ResponseEntity<List<SuspiciousActivity>> getSuspiciousActivities(@PathVariable String userId) {
        List<SuspiciousActivity> activities = antiCheatService.getSuspiciousActivities(userId);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get all unresolved suspicious activities (GM)
     */
    @GetMapping("/suspicious/unresolved")
    public ResponseEntity<List<SuspiciousActivity>> getUnresolvedActivities() {
        List<SuspiciousActivity> activities = antiCheatService.getUnresolvedActivities();
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Analyze player behavior
     */
    @PostMapping("/analyze/{userId}")
    public ResponseEntity<Map<String, Object>> analyzePlayerBehavior(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int days) {
        
        Map<String, Object> analysis = antiCheatService.analyzePlayerBehavior(userId, days);
        return ResponseEntity.ok(analysis);
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "anti-cheat-service"
        ));
    }
}
