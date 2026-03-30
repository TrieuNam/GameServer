package com.SouthMillion.leaderboard_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Ranking Service - Manages leaderboard rankings using Redis sorted sets
 * Provides fast ranking updates and queries for arena, trial, and global rankings
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String ARENA_RANKING_KEY = "ranking:arena:global";
    private static final String ARENA_SEASON_KEY_PREFIX = "ranking:arena:season:";
    private static final String TRIAL_RANKING_KEY_PREFIX = "ranking:trial:";
    private static final String TRIAL_GLOBAL_KEY = "ranking:trial:global";
    private static final long RANKING_TTL_DAYS = 90; // 3 months

    /**
     * Update arena ranking for a player
     */
    public void updateArenaRanking(String roleId, String playerName, int rating, boolean isWinner) {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            
            // Store player info: roleId:playerName
            String member = roleId + ":" + playerName;
            
            // Update global arena ranking (sorted by rating)
            zSetOps.add(ARENA_RANKING_KEY, member, rating);
            
            // Set expiration on the ranking key
            redisTemplate.expire(ARENA_RANKING_KEY, RANKING_TTL_DAYS, TimeUnit.DAYS);
            
            // Get player's rank
            Long rank = zSetOps.reverseRank(ARENA_RANKING_KEY, member);
            
            log.info("Updated arena ranking: roleId={}, rating={}, rank={}, isWinner={}", 
                    roleId, rating, rank != null ? rank + 1 : "N/A", isWinner);
        } catch (Exception e) {
            log.error("Failed to update arena ranking for roleId: {}", roleId, e);
        }
    }

    /**
     * Update season-specific arena ranking
     */
    public void updateSeasonRanking(String roleId, String seasonId, int rating) {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            String seasonKey = ARENA_SEASON_KEY_PREFIX + seasonId;
            
            zSetOps.add(seasonKey, roleId, rating);
            redisTemplate.expire(seasonKey, RANKING_TTL_DAYS, TimeUnit.DAYS);
            
            Long rank = zSetOps.reverseRank(seasonKey, roleId);
            log.debug("Updated season {} ranking: roleId={}, rating={}, rank={}", 
                    seasonId, roleId, rating, rank != null ? rank + 1 : "N/A");
        } catch (Exception e) {
            log.error("Failed to update season ranking for roleId: {}", roleId, e);
        }
    }

    /**
     * Update trial ranking for specific trial
     */
    public void updateTrialRanking(String roleId, int trialId, long score, int stars, long completionTime) {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            String trialKey = TRIAL_RANKING_KEY_PREFIX + trialId;
            
            // Score composite: stars * 1000000 + score - completionTime/1000
            // Higher stars > higher score > faster time
            double compositeScore = (stars * 1000000.0) + score - (completionTime / 1000.0);
            
            String member = roleId + ":" + stars + ":" + score + ":" + completionTime;
            zSetOps.add(trialKey, member, compositeScore);
            redisTemplate.expire(trialKey, RANKING_TTL_DAYS, TimeUnit.DAYS);
            
            Long rank = zSetOps.reverseRank(trialKey, member);
            log.info("Updated trial {} ranking: roleId={}, score={}, stars={}, time={}ms, rank={}", 
                    trialId, roleId, score, stars, completionTime, rank != null ? rank + 1 : "N/A");
        } catch (Exception e) {
            log.error("Failed to update trial ranking for roleId: {} trialId: {}", roleId, trialId, e);
        }
    }

    /**
     * Update global trial score ranking
     */
    public void updateGlobalTrialRanking(String roleId, long totalScore) {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            
            // Increment player's total trial score
            zSetOps.incrementScore(TRIAL_GLOBAL_KEY, roleId, totalScore);
            redisTemplate.expire(TRIAL_GLOBAL_KEY, RANKING_TTL_DAYS, TimeUnit.DAYS);
            
            Double currentScore = zSetOps.score(TRIAL_GLOBAL_KEY, roleId);
            Long rank = zSetOps.reverseRank(TRIAL_GLOBAL_KEY, roleId);
            
            log.debug("Updated global trial ranking: roleId={}, totalScore={}, rank={}", 
                    roleId, currentScore, rank != null ? rank + 1 : "N/A");
        } catch (Exception e) {
            log.error("Failed to update global trial ranking for roleId: {}", roleId, e);
        }
    }

    /**
     * Get top N players from arena ranking
     */
    public Set<ZSetOperations.TypedTuple<String>> getTopArenaPlayers(int topN) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        return zSetOps.reverseRangeWithScores(ARENA_RANKING_KEY, 0, topN - 1);
    }

    /**
     * Get player's arena rank
     */
    public Long getArenaRank(String roleId) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        // Find member starting with roleId
        Set<String> members = zSetOps.range(ARENA_RANKING_KEY, 0, -1);
        if (members != null) {
            for (String member : members) {
                if (member.startsWith(roleId + ":")) {
                    Long rank = zSetOps.reverseRank(ARENA_RANKING_KEY, member);
                    return rank != null ? rank + 1 : null; // 1-based rank
                }
            }
        }
        return null;
    }

    /**
     * Get player's arena rating
     */
    public Double getArenaRating(String roleId) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        
        Set<String> members = zSetOps.range(ARENA_RANKING_KEY, 0, -1);
        if (members != null) {
            for (String member : members) {
                if (member.startsWith(roleId + ":")) {
                    return zSetOps.score(ARENA_RANKING_KEY, member);
                }
            }
        }
        return null;
    }

    /**
     * Get top N players from specific trial
     */
    public Set<ZSetOperations.TypedTuple<String>> getTopTrialPlayers(int trialId, int topN) {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        String trialKey = TRIAL_RANKING_KEY_PREFIX + trialId;
        return zSetOps.reverseRangeWithScores(trialKey, 0, topN - 1);
    }
}
