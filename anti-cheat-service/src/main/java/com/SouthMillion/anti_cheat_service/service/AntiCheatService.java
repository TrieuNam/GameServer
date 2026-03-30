package com.SouthMillion.anti_cheat_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.anti_cheat_service.entity.CheatReport;
import com.SouthMillion.anti_cheat_service.entity.PlayerBehavior;
import com.SouthMillion.anti_cheat_service.entity.SuspiciousActivity;
import com.SouthMillion.anti_cheat_service.repository.CheatReportRepository;
import com.SouthMillion.anti_cheat_service.repository.PlayerBehaviorRepository;
import com.SouthMillion.anti_cheat_service.repository.SuspiciousActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AntiCheatService {
    
    private final CheatReportRepository cheatReportRepository;
    private final PlayerBehaviorRepository playerBehaviorRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${anticheat.detection.speed-hack-threshold}")
    private double speedHackThreshold;
    
    @Value("${anticheat.detection.teleport-distance}")
    private double teleportDistance;
    
    @Value("${anticheat.detection.damage-hack-threshold}")
    private double damageHackThreshold;
    
    @Value("${anticheat.detection.resource-gain-threshold}")
    private double resourceGainThreshold;
    
    @Value("${anticheat.analysis.behavior-window}")
    private int behaviorWindow;
    
    @Value("${anticheat.analysis.suspicious-score-threshold}")
    private int suspiciousScoreThreshold;
    
    @Value("${anticheat.analysis.auto-ban-score}")
    private int autoBanScore;
    
    /**
     * Report movement data for speed/teleport detection
     */
    @Transactional
    public void reportMovement(String userId, double x, double y, double z, double speed) {
        String posKey = "player:position:" + userId;
        String lastPos = redisTemplate.opsForValue().get(posKey);
        
        if (lastPos != null) {
            try {
                String[] parts = lastPos.split(",");
                double lastX = Double.parseDouble(parts[0]);
                double lastY = Double.parseDouble(parts[1]);
                double lastZ = Double.parseDouble(parts[2]);
                long lastTime = Long.parseLong(parts[3]);
                
                // Calculate distance and time
                double distance = Math.sqrt(
                    Math.pow(x - lastX, 2) + 
                    Math.pow(y - lastY, 2) + 
                    Math.pow(z - lastZ, 2)
                );
                long timeDiff = System.currentTimeMillis() - lastTime;
                double actualSpeed = distance / (timeDiff / 1000.0);
                
                // Check for teleport
                if (distance > teleportDistance && timeDiff < 1000) {
                    createCheatReport(userId, "TELEPORT", 4, 0.95,
                        Map.of("distance", distance, "time", timeDiff, "from", lastPos, "to", x + "," + y + "," + z));
                    log.warn("Teleport detected: userId={}, distance={}", userId, distance);
                }
                
                // Check for speed hack
                if (actualSpeed > speed * speedHackThreshold) {
                    createCheatReport(userId, "SPEED_HACK", 3, 0.85,
                        Map.of("actualSpeed", actualSpeed, "expectedSpeed", speed, "ratio", actualSpeed / speed));
                    log.warn("Speed hack detected: userId={}, actual={}, expected={}", userId, actualSpeed, speed);
                }
                
                // Record behavior
                recordBehavior(userId, "MOVEMENT_SPEED", actualSpeed, speed);
                
            } catch (Exception e) {
                log.error("Error analyzing movement: {}", e.getMessage(), e);
            }
        }
        
        // Update position
        String newPos = x + "," + y + "," + z + "," + System.currentTimeMillis();
        redisTemplate.opsForValue().set(posKey, newPos, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Report damage dealt for damage hack detection
     */
    @Transactional
    public void reportDamage(String userId, String targetId, double damage, double expectedDamage) {
        if (damage > expectedDamage * damageHackThreshold) {
            double ratio = damage / expectedDamage;
            createCheatReport(userId, "DAMAGE_HACK", 3, Math.min(0.9, 0.5 + (ratio - 2) * 0.1),
                Map.of("damage", damage, "expected", expectedDamage, "ratio", ratio, "target", targetId));
            log.warn("Damage hack detected: userId={}, damage={}, expected={}", userId, damage, expectedDamage);
        }
        
        recordBehavior(userId, "DAMAGE_DEALT", damage, expectedDamage);
    }
    
    /**
     * Report resource gain for resource hack detection
     */
    @Transactional
    public void reportResourceGain(String userId, String resourceType, long amount, long expectedAmount) {
        if (amount > expectedAmount * resourceGainThreshold) {
            double ratio = (double) amount / expectedAmount;
            createCheatReport(userId, "RESOURCE_HACK", 4, Math.min(0.95, 0.6 + (ratio - 10) * 0.05),
                Map.of("resource", resourceType, "amount", amount, "expected", expectedAmount, "ratio", ratio));
            log.warn("Resource hack detected: userId={}, resource={}, amount={}, expected={}", 
                userId, resourceType, amount, expectedAmount);
        }
        
        recordBehavior(userId, "RESOURCE_GAINED", (double) amount, (double) expectedAmount);
    }
    
    /**
     * Record player behavior for statistical analysis
     */
    @Transactional
    public void recordBehavior(String userId, String metricType, double value, double expectedValue) {
        PlayerBehavior behavior = new PlayerBehavior();
        behavior.setUserId(userId);
        behavior.setMetricType(metricType);
        behavior.setMetricValue(value);
        behavior.setExpectedValue(expectedValue);
        
        // Calculate deviation
        double deviation = expectedValue > 0 ? (value - expectedValue) / expectedValue : 0;
        behavior.setDeviation(deviation);
        
        // Check if anomaly (> 2 standard deviations or > 50% deviation)
        boolean isAnomaly = Math.abs(deviation) > 0.5;
        behavior.setIsAnomaly(isAnomaly);
        
        playerBehaviorRepository.save(behavior);
        
        // If anomaly, increment suspicion
        if (isAnomaly) {
            incrementSuspicionScore(userId, (int) (Math.abs(deviation) * 10));
        }
    }
    
    /**
     * Create cheat report
     */
    @Transactional
    public CheatReport createCheatReport(String userId, String cheatType, int severity, double confidence, Map<String, Object> evidence) {
        try {
            CheatReport report = new CheatReport();
            report.setUserId(userId);
            report.setCheatType(cheatType);
            report.setSeverity(severity);
            report.setConfidence(confidence);
            report.setEvidence(objectMapper.writeValueAsString(evidence));
            report.setStatus("DETECTED");
            
            CheatReport saved = cheatReportRepository.save(report);
            
            // Create suspicious activity
            createSuspiciousActivity(userId, cheatType, severity * 20, 
                "Cheat detected: " + cheatType, evidence);
            
            // Check if should auto-ban
            checkAutoBan(userId);
            
            // Publish to Kafka
            kafkaTemplate.send("cheat-detected", userId.toString(), 
                objectMapper.writeValueAsString(Map.of(
                    "reportId", saved.getId(),
                    "userId", userId,
                    "cheatType", cheatType,
                    "severity", severity,
                    "confidence", confidence
                ))
            );
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error creating cheat report: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create suspicious activity
     */
    @Transactional
    public SuspiciousActivity createSuspiciousActivity(String userId, String activityType, int suspicionScore,
                                                       String description, Map<String, Object> details) {
        try {
            SuspiciousActivity activity = new SuspiciousActivity();
            activity.setUserId(userId);
            activity.setActivityType(activityType);
            activity.setSuspicionScore(suspicionScore);
            activity.setDescription(description);
            activity.setDetails(objectMapper.writeValueAsString(details));
            activity.setIsResolved(false);
            
            return suspiciousActivityRepository.save(activity);
            
        } catch (Exception e) {
            log.error("Error creating suspicious activity: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Increment user's suspicion score
     */
    private void incrementSuspicionScore(String userId, int points) {
        String key = "suspicion:score:" + userId;
        redisTemplate.opsForValue().increment(key, points);
        redisTemplate.expire(key, behaviorWindow, TimeUnit.SECONDS);
        
        // Get current score
        String scoreStr = redisTemplate.opsForValue().get(key);
        int totalScore = scoreStr != null ? Integer.parseInt(scoreStr) : 0;
        
        log.debug("Suspicion score updated: userId={}, points={}, total={}", userId, points, totalScore);
        
        // Alert if threshold exceeded
        if (totalScore >= suspiciousScoreThreshold) {
            log.warn("High suspicion score: userId={}, score={}", userId, totalScore);
            createSuspiciousActivity(userId, "HIGH_SUSPICION_SCORE", totalScore,
                "Player has accumulated high suspicion score", Map.of("score", totalScore));
        }
    }
    
    /**
     * Check if user should be auto-banned
     */
    private void checkAutoBan(String userId) {
        try {
            // Check recent confirmed cheats
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            Long confirmedCheats = cheatReportRepository.countConfirmedCheatsByUserSince(userId, oneDayAgo);
            
            // Check suspicion score
            Long totalSuspicion = suspiciousActivityRepository.calculateTotalSuspicionScore(
                userId, LocalDateTime.now().minusSeconds(behaviorWindow));
            int suspicionScore = totalSuspicion != null ? totalSuspicion.intValue() : 0;
            
            // Auto-ban conditions
            boolean shouldBan = confirmedCheats >= 3 || suspicionScore >= autoBanScore;
            
            if (shouldBan) {
                log.warn("Auto-ban triggered: userId={}, confirmedCheats={}, suspicionScore={}", 
                    userId, confirmedCheats, suspicionScore);
                
                // Publish ban event
                kafkaTemplate.send("player-ban", userId.toString(),
                    objectMapper.writeValueAsString(Map.of(
                        "userId", userId,
                        "reason", "Automatic ban - cheat detection",
                        "confirmedCheats", confirmedCheats,
                        "suspicionScore", suspicionScore,
                        "duration", 86400 // 24 hours
                    ))
                );
            }
            
        } catch (Exception e) {
            log.error("Error checking auto-ban: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Review cheat report (GM action)
     */
    @Transactional
    public CheatReport reviewCheatReport(Long reportId, Long gmId, String status, String action, String notes) {
        CheatReport report = cheatReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        
        report.setStatus(status.toUpperCase());
        report.setActionTaken(action.toUpperCase());
        report.setReviewedBy(gmId);
        report.setReviewedAt(LocalDateTime.now());
        report.setNotes(notes);
        
        log.info("Cheat report reviewed: reportId={}, status={}, action={}, gmId={}", 
            reportId, status, action, gmId);
        
        return cheatReportRepository.save(report);
    }
    
    /**
     * Get user's cheat history
     */
    public List<CheatReport> getUserCheatHistory(String userId) {
        return cheatReportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get pending cheat reports
     */
    public List<CheatReport> getPendingReports() {
        return cheatReportRepository.findByStatusOrderByCreatedAtDesc("DETECTED");
    }
    
    /**
     * Get high severity reports
     */
    public List<CheatReport> getHighSeverityReports(int minSeverity) {
        return cheatReportRepository.findHighSeverityUnreviewedReports(minSeverity);
    }
    
    /**
     * Get suspicious activities
     */
    public List<SuspiciousActivity> getSuspiciousActivities(String userId) {
        return suspiciousActivityRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get unresolved suspicious activities
     */
    public List<SuspiciousActivity> getUnresolvedActivities() {
        return suspiciousActivityRepository.findByIsResolvedOrderByCreatedAtDesc(false);
    }
    
    /**
     * Analyze player behavior patterns
     */
    public Map<String, Object> analyzePlayerBehavior(String userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<PlayerBehavior> behaviors = playerBehaviorRepository.findRecentBehaviorsByUserId(userId, since);
        
        Map<String, DescriptiveStatistics> statsByMetric = new HashMap<>();
        
        for (PlayerBehavior behavior : behaviors) {
            statsByMetric.computeIfAbsent(behavior.getMetricType(), k -> new DescriptiveStatistics())
                .addValue(behavior.getDeviation());
        }
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("userId", userId);
        analysis.put("period", days + " days");
        analysis.put("totalSamples", behaviors.size());
        
        Map<String, Map<String, Double>> metricStats = new HashMap<>();
        for (Map.Entry<String, DescriptiveStatistics> entry : statsByMetric.entrySet()) {
            DescriptiveStatistics stats = entry.getValue();
            metricStats.put(entry.getKey(), Map.of(
                "mean", stats.getMean(),
                "stdDev", stats.getStandardDeviation(),
                "max", stats.getMax(),
                "min", stats.getMin(),
                "samples", (double) stats.getN()
            ));
        }
        analysis.put("metrics", metricStats);
        
        // Count anomalies
        Long anomalyCount = playerBehaviorRepository.countAnomaliesByUserSince(userId, since);
        analysis.put("anomalyCount", anomalyCount);
        analysis.put("anomalyRate", behaviors.size() > 0 ? (double) anomalyCount / behaviors.size() : 0.0);
        
        return analysis;
    }
}
