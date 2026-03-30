package com.SouthMillion.task_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Statistics Service - Using Redis for persistent statistics
 * Aggregates player performance statistics from trial, arena, combat systems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final String STATS_KEY_PREFIX = "task:stats:"; // task:stats:{roleId} -> PlayerStatistics
    private static final long STATS_TTL_DAYS = 365; // Keep stats for 1 year

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Update trial statistics
     */
    public void updateTrialStats(String roleId, boolean isVictory, int stars, int stageReached, long completionTime) {
        PlayerStatistics stats = getOrCreateStats(roleId);

        stats.trialAttempts++;
        if (isVictory) {
            stats.trialWins++;
            stats.totalStars += stars;
            stats.totalCompletionTime += completionTime;

            // Update best time
            if (stats.bestCompletionTime == 0 || completionTime < stats.bestCompletionTime) {
                stats.bestCompletionTime = completionTime;
            }

            // Update highest stage
            if (stageReached > stats.highestStageReached) {
                stats.highestStageReached = stageReached;
            }
        }

        saveStats(roleId, stats);

        log.debug("Updated trial stats for roleId={}: attempts={}, wins={}, winRate={}%",
                roleId, stats.trialAttempts, stats.trialWins, stats.getTrialWinRate());
    }

    /**
     * Update arena statistics
     */
    public void updateArenaStats(String roleId, boolean isWinner, boolean isPerfectVictory, 
                                 int ratingChange, long matchDuration, long totalDamage) {
        PlayerStatistics stats = getOrCreateStats(roleId);

        stats.arenaMatches++;
        if (isWinner) {
            stats.arenaWins++;
            stats.currentWinStreak++;

            // Update max win streak
            if (stats.currentWinStreak > stats.maxWinStreak) {
                stats.maxWinStreak = stats.currentWinStreak;
            }

            if (isPerfectVictory) {
                stats.perfectVictories++;
            }
        } else {
            stats.arenaLosses++;
            stats.currentWinStreak = 0; // Reset win streak
        }

        stats.totalArenaDamage += totalDamage;
        stats.totalMatchDuration += matchDuration;
        stats.currentRating += ratingChange;

        // Update peak rating
        if (stats.currentRating > stats.peakRating) {
            stats.peakRating = stats.currentRating;
        }

        saveStats(roleId, stats);

        log.debug("Updated arena stats for roleId={}: matches={}, wins={}, winRate={}%, rating={}",
                roleId, stats.arenaMatches, stats.arenaWins, 
                stats.getArenaWinRate(), stats.currentRating);
    }

    /**
     * Update combat statistics
     */
    public void updateCombatStats(String roleId, boolean isVictory, int killCount, int deathCount,
                                  long totalDamage, long totalHealing, int comboMax) {
        PlayerStatistics stats = getOrCreateStats(roleId);

        stats.combatCount++;
        if (isVictory) {
            stats.combatWins++;
        }

        stats.totalKills += killCount;
        stats.totalDeaths += deathCount;
        stats.totalDamageDealt += totalDamage;
        stats.totalHealingDone += totalHealing;

        // Update max combo
        if (comboMax > stats.maxCombo) {
            stats.maxCombo = comboMax;
        }

        saveStats(roleId, stats);

        log.debug("Updated combat stats for roleId={}: combats={}, wins={}, kills={}, deaths={}, KD={}",
                roleId, stats.combatCount, stats.combatWins, 
                stats.totalKills, stats.totalDeaths, stats.getKillDeathRatio());
    }

    /**
     * Get statistics for a player
     */
    public PlayerStatistics getStats(String roleId) {
        String key = STATS_KEY_PREFIX + roleId;
        Object statsObj = redisTemplate.opsForValue().get(key);
        
        if (statsObj instanceof PlayerStatistics) {
            return (PlayerStatistics) statsObj;
        }
        return null;
    }

    /**
     * Get or create player statistics
     */
    private PlayerStatistics getOrCreateStats(String roleId) {
        PlayerStatistics stats = getStats(roleId);
        if (stats == null) {
            stats = new PlayerStatistics(roleId);
        }
        return stats;
    }

    /**
     * Save statistics to Redis
     */
    private void saveStats(String roleId, PlayerStatistics stats) {
        String key = STATS_KEY_PREFIX + roleId;
        redisTemplate.opsForValue().set(key, stats, STATS_TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * Player statistics data structure
     */
    @Data
    public static class PlayerStatistics {
        private String roleId;

        // Trial statistics
        private int trialAttempts = 0;
        private int trialWins = 0;
        private int totalStars = 0;
        private int highestStageReached = 0;
        private long totalCompletionTime = 0;
        private long bestCompletionTime = 0;

        // Arena statistics
        private int arenaMatches = 0;
        private int arenaWins = 0;
        private int arenaLosses = 0;
        private int currentWinStreak = 0;
        private int maxWinStreak = 0;
        private int perfectVictories = 0;
        private int currentRating = 1000; // Starting rating
        private int peakRating = 1000;
        private long totalArenaDamage = 0;
        private long totalMatchDuration = 0;

        // Combat statistics
        private int combatCount = 0;
        private int combatWins = 0;
        private int totalKills = 0;
        private int totalDeaths = 0;
        private long totalDamageDealt = 0;
        private long totalHealingDone = 0;
        private int maxCombo = 0;

        public PlayerStatistics() {}

        public PlayerStatistics(String roleId) {
            this.roleId = roleId;
        }

        // Calculated metrics
        public double getTrialWinRate() {
            return trialAttempts > 0 ? (trialWins * 100.0 / trialAttempts) : 0.0;
        }

        public double getArenaWinRate() {
            return arenaMatches > 0 ? (arenaWins * 100.0 / arenaMatches) : 0.0;
        }

        public double getCombatWinRate() {
            return combatCount > 0 ? (combatWins * 100.0 / combatCount) : 0.0;
        }

        public double getKillDeathRatio() {
            return totalDeaths > 0 ? (totalKills * 1.0 / totalDeaths) : totalKills;
        }

        public double getAverageDamagePerMatch() {
            return arenaMatches > 0 ? (totalArenaDamage * 1.0 / arenaMatches) : 0.0;
        }

        public double getAverageMatchDuration() {
            return arenaMatches > 0 ? (totalMatchDuration * 1.0 / arenaMatches / 1000.0) : 0.0; // seconds
        }
    }
}
