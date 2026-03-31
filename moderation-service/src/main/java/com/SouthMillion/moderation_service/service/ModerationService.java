package com.SouthMillion.moderation_service.service;

import com.SouthMillion.moderation_service.entity.Report;
import com.SouthMillion.moderation_service.entity.Violation;
import com.SouthMillion.moderation_service.repository.ReportRepository;
import com.SouthMillion.moderation_service.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {
    
    private final ReportRepository reportRepository;
    private final ViolationRepository violationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${moderation.bad-words}")
    private String badWordsStr;
    
    @Value("${moderation.max-violations:3}")
    private int maxViolations;
    
    @Value("${moderation.ban-duration-hours:24}")
    private int banDurationHours;
    
    @Value("${moderation.spam-threshold:5}")
    private int spamThreshold;
    
    @Value("${moderation.spam-window-seconds:10}")
    private int spamWindowSeconds;
    
    private Set<String> badWords;
    
    /**
     * Filter chat message for bad words
     */
    public Map<String, Object> filterMessage(String userId, String message) {
        if (badWords == null) {
            badWords = new HashSet<>(Arrays.asList(badWordsStr.toLowerCase().split(",")));
        }
        
        // Check if user is banned/muted
        if (isUserMuted(userId)) {
            return Map.of("allowed", false, "reason", "User is muted", "filtered", message);
        }
        
        // Check spam
        if (isSpam(userId)) {
            recordViolation(userId, "SPAM", message, 2);
            return Map.of("allowed", false, "reason", "Spam detected", "filtered", message);
        }
        
        // Filter bad words
        String filtered = message;
        boolean hasBadWords = false;
        String lowerMessage = message.toLowerCase();
        
        for (String badWord : badWords) {
            if (lowerMessage.contains(badWord)) {
                hasBadWords = true;
                filtered = filtered.replaceAll("(?i)" + badWord, "***");
            }
        }
        
        if (hasBadWords) {
            recordViolation(userId, "BAD_WORD", message, 1);
        }
        
        recordMessage(userId);
        
        return Map.of(
            "allowed", true,
            "filtered", filtered,
            "hasBadWords", hasBadWords
        );
    }
    
    /**
     * Create user report
     */
    @Transactional
    public Report createReport(String reporterId, String reportedUserId, String reportType,
                              String content, String evidence) {
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setReportedUserId(reportedUserId);
        report.setReportType(reportType);
        report.setContent(content);
        report.setEvidence(evidence);
        report.setStatus("PENDING");
        
        Report saved = reportRepository.save(report);
        log.info("Report created: {} reported {} for {}", reporterId, reportedUserId, reportType);
        
        // Auto-ban if too many reports
        Long reportCount = reportRepository.countByReportedUserIdAndStatus(reportedUserId, "PENDING");
        if (reportCount >= 5) {
            autoModerate(reportedUserId, "Multiple reports received");
        }
        
        return saved;
    }
    
    /**
     * Handle report
     */
    @Transactional
    public void handleReport(Long reportId, String gmId, String resolution, String action) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));
        
        report.setStatus("RESOLVED");
        report.setResolution(resolution);
        report.setHandledBy(gmId);
        report.setHandledAt(LocalDateTime.now());
        reportRepository.save(report);
        
        // Take action if needed
        if ("BAN".equals(action)) {
            banUser(report.getReportedUserId(), banDurationHours, "GM action: " + resolution);
        } else if ("MUTE".equals(action)) {
            muteUser(report.getReportedUserId(), 24, "GM action: " + resolution);
        }
    }
    
    /**
     * Record violation
     */
    @Transactional
    public void recordViolation(String userId, String violationType, String content, int severity) {
        Violation violation = new Violation();
        violation.setUserId(userId);
        violation.setViolationType(violationType);
        violation.setContent(content);
        violation.setSeverity(severity);
        
        // Check total violations
        Long violationCount = violationRepository.countByUserIdAndCreatedAtAfter(
            userId, LocalDateTime.now().minusDays(7)
        );
        
        if (violationCount >= maxViolations) {
            violation.setActionTaken("BAN");
            violation.setDurationHours(banDurationHours);
            violation.setExpiresAt(LocalDateTime.now().plusHours(banDurationHours));
            banUser(userId, banDurationHours, "Auto-ban: " + violationCount + " violations");
        } else if (violationCount >= 2) {
            violation.setActionTaken("MUTE");
            violation.setDurationHours(1);
            violation.setExpiresAt(LocalDateTime.now().plusHours(1));
            muteUser(userId, 1, "Auto-mute: repeated violations");
        } else {
            violation.setActionTaken("WARNING");
        }
        
        violationRepository.save(violation);
    }
    
    /**
     * Check if user is muted
     */
    public boolean isUserMuted(String userId) {
        String key = "muted:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Check if user is banned
     */
    public boolean isUserBanned(String userId) {
        String key = "banned:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Mute user
     */
    public void muteUser(String userId, int hours, String reason) {
        String key = "muted:" + userId;
        redisTemplate.opsForValue().set(key, reason, hours, TimeUnit.HOURS);
        log.info("User {} muted for {} hours: {}", userId, hours, reason);
    }
    
    /**
     * Ban user
     */
    public void banUser(String userId, int hours, String reason) {
        String key = "banned:" + userId;
        redisTemplate.opsForValue().set(key, reason, hours, TimeUnit.HOURS);
        log.warn("User {} banned for {} hours: {}", userId, hours, reason);
    }
    
    /**
     * Check spam
     */
    private boolean isSpam(String userId) {
        String key = "msg:count:" + userId;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (count >= spamThreshold) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Record message for spam detection
     */
    private void recordMessage(String userId) {
        String key = "msg:count:" + userId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, spamWindowSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * Auto moderate user
     */
    private void autoModerate(String userId, String reason) {
        recordViolation(userId, "AUTO_MODERATION", reason, 3);
        banUser(userId, banDurationHours, reason);
    }
    
    /**
     * Get user violations
     */
    public List<Violation> getUserViolations(String userId) {
        return violationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get pending reports
     */
    public List<Report> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }
}
