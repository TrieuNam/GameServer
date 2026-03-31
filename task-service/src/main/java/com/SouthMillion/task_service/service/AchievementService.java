package com.SouthMillion.task_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.task_service.client.BagClient;
import com.SouthMillion.task_service.client.WalletClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Achievement Service - Using Redis for persistent achievement tracking
 * Integrates with Kafka events from trial, arena, combat systems
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private static final String ACHIEVEMENT_KEY_PREFIX = "task:achievement:"; // task:achievement:{roleId} -> Map<achievementId, AchievementProgress>
    private static final long ACHIEVEMENT_TTL_DAYS = 365; // Keep achievements for 1 year

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WalletClient walletClient;
    private final BagClient bagClient;

    /** Kafka for achievement unlock notifications (webSocket-server listens on topic). */
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ACHIEVEMENT_UNLOCK_TOPIC = "task.achievement.unlocked";
    
    // Achievement rewards configuration (achievementId -> AchievementReward)
    private static final Map<String, AchievementReward> ACHIEVEMENT_REWARDS = new HashMap<>();
    
    static {
        // Trial achievements
        ACHIEVEMENT_REWARDS.put("FIRST_TRIAL_VICTORY", new AchievementReward(100L, 50L, Arrays.asList(
            new ItemReward(1001, 5) // 5x Health Potion
        )));
        ACHIEVEMENT_REWARDS.put("PERFECT_TRIALS", new AchievementReward(500L, 200L, Arrays.asList(
            new ItemReward(1002, 10) // 10x Mana Potion
        )));
        ACHIEVEMENT_REWARDS.put("TRIAL_MASTER", new AchievementReward(5000L, 2000L, Arrays.asList(
            new ItemReward(2001, 1), // 1x Legendary Weapon
            new ItemReward(1003, 50)  // 50x Experience Scroll
        )));
        ACHIEVEMENT_REWARDS.put("RECORD_BREAKER", new AchievementReward(2000L, 1000L, Collections.emptyList()));
        
        // Arena achievements
        ACHIEVEMENT_REWARDS.put("FIRST_ARENA_VICTORY", new AchievementReward(200L, 100L, Arrays.asList(
            new ItemReward(1004, 3) // 3x Arena Token
        )));
        ACHIEVEMENT_REWARDS.put("ARENA_CHAMPION", new AchievementReward(1000L, 500L, Arrays.asList(
            new ItemReward(2002, 1) // 1x Champion Crown
        )));
        ACHIEVEMENT_REWARDS.put("UNDEFEATED_WARRIOR", new AchievementReward(3000L, 1500L, Arrays.asList(
            new ItemReward(2003, 1) // 1x Warrior Title
        )));
        ACHIEVEMENT_REWARDS.put("ARENA_MASTER", new AchievementReward(10000L, 5000L, Arrays.asList(
            new ItemReward(2004, 1), // 1x Master Badge
            new ItemReward(1005, 100) // 100x Glory Points
        )));
        
        // Combat achievements
        ACHIEVEMENT_REWARDS.put("FIRST_MONSTER_KILL", new AchievementReward(50L, 20L, Collections.emptyList()));
        ACHIEVEMENT_REWARDS.put("MONSTER_HUNTER", new AchievementReward(2000L, 800L, Arrays.asList(
            new ItemReward(1006, 20) // 20x Monster Material
        )));
        ACHIEVEMENT_REWARDS.put("BOSS_SLAYER", new AchievementReward(5000L, 2500L, Arrays.asList(
            new ItemReward(2005, 1) // 1x Boss Trophy
        )));
    }

    /**
     * Check and unlock trial-related achievements
     */
    public void checkTrialAchievements(String roleId, int trialId, int stars, boolean isNewRecord) {
        log.debug("Checking trial achievements for roleId={}, trialId={}, stars={}", roleId, trialId, stars);

        // First trial completion
        if (isFirstTrialCompletion(roleId)) {
            unlockAchievement(roleId, "FIRST_TRIAL_VICTORY", "Complete your first trial");
        }

        // Perfect 3-star trial
        if (stars == 3) {
            incrementAchievement(roleId, "PERFECT_TRIALS", "Complete 10 trials with 3 stars", 10);
        }

        // Trial master (100 trials)
        incrementAchievement(roleId, "TRIAL_MASTER", "Complete 100 trials", 100);

        // New record breaker
        if (isNewRecord) {
            incrementAchievement(roleId, "RECORD_BREAKER", "Set 50 new records", 50);
        }
    }

    /**
     * Check and unlock arena-related achievements
     */
    public void checkArenaAchievements(String roleId, boolean isWinner, boolean isPerfectVictory, 
                                       int winStreak, long totalDamage) {
        log.debug("Checking arena achievements for roleId={}, winner={}, perfect={}", 
                roleId, isWinner, isPerfectVictory);

        if (isWinner) {
            // First arena win
            if (isFirstArenaWin(roleId)) {
                unlockAchievement(roleId, "FIRST_ARENA_VICTORY", "Win your first arena match");
            }

            // Perfect victory achievement
            if (isPerfectVictory) {
                incrementAchievement(roleId, "PERFECT_WARRIOR", "Win 25 arena matches without taking damage", 25);
            }

            // Win streak achievements
            if (winStreak >= 5) {
                unlockAchievement(roleId, "WIN_STREAK_5", "Win 5 arena matches in a row");
            }
            if (winStreak >= 10) {
                unlockAchievement(roleId, "WIN_STREAK_10", "Win 10 arena matches in a row");
            }

            // Arena champion (100 wins)
            incrementAchievement(roleId, "ARENA_CHAMPION", "Win 100 arena matches", 100);

            // High damage achievement
            if (totalDamage > 100000) {
                incrementAchievement(roleId, "HIGH_DAMAGE_DEALER", "Deal 100K+ damage in 50 matches", 50);
            }
        }

        // Participation achievement (win or lose)
        incrementAchievement(roleId, "ARENA_VETERAN", "Participate in 500 arena matches", 500);
    }

    /**
     * Check and unlock combat-related achievements
     */
    public void checkCombatAchievements(String roleId, boolean isVictory, int killCount, 
                                       int comboMax, long totalDamage) {
        log.debug("Checking combat achievements for roleId={}, victory={}, kills={}, combo={}", 
                roleId, isVictory, killCount, comboMax);

        if (isVictory) {
            // Kill count achievements
            if (killCount >= 10) {
                incrementAchievement(roleId, "MONSTER_SLAYER", "Kill 1000 monsters", 1000);
            }
            if (killCount >= 50) {
                incrementAchievement(roleId, "MONSTER_HUNTER", "Kill 5000 monsters", 5000);
            }

            // Combo achievements
            if (comboMax >= 50) {
                unlockAchievement(roleId, "COMBO_MASTER_50", "Achieve 50 hit combo");
            }
            if (comboMax >= 100) {
                unlockAchievement(roleId, "COMBO_MASTER_100", "Achieve 100 hit combo");
            }

            // Damage milestones
            if (totalDamage >= 50000) {
                incrementAchievement(roleId, "HEAVY_HITTER", "Deal 50K+ damage in 100 combats", 100);
            }
        }
    }

    /**
     * Unlock achievement immediately
     */
    private void unlockAchievement(String roleId, String achievementId, String description) {
        Map<String, AchievementProgress> achievements = getPlayerAchievements(roleId);

        if (!achievements.containsKey(achievementId)) {
            AchievementProgress progress = new AchievementProgress(achievementId, description, 1, 1, true);
            achievements.put(achievementId, progress);
            savePlayerAchievements(roleId, achievements);
            
            log.info("🏆 Achievement unlocked for roleId={}: {} - {}", roleId, achievementId, description);
            
            // Send WebSocket notification (basic logging for now)
            try {
                notifyPlayerAchievement(roleId, achievementId, description);
            } catch (Exception e) {
                log.warn("Failed to notify achievement: {}", e.getMessage());
            }
            
            // Grant achievement rewards
            try {
                grantAchievementRewards(roleId, achievementId);
            } catch (Exception e) {
                log.error("Failed to grant achievement rewards: {}", e.getMessage());
            }
        }
    }

    /**
     * Increment achievement progress
     */
    private void incrementAchievement(String roleId, String achievementId, String description, int target) {
        Map<String, AchievementProgress> achievements = getPlayerAchievements(roleId);

        AchievementProgress progress = achievements.computeIfAbsent(achievementId,
                k -> new AchievementProgress(achievementId, description, 0, target, false));

        progress.increment();

        if (progress.isCompleted() && !progress.isUnlocked()) {
            progress.setUnlocked(true);
            log.info("🏆 Achievement unlocked for roleId={}: {} - {} ({}/{})", 
                    roleId, achievementId, description, progress.getCurrent(), progress.getTarget());
            
            // Save achievements after unlock
            savePlayerAchievements(roleId, achievements);
            
            // Send notification to player
            try {
                notifyPlayerAchievement(roleId, achievementId, description);
            } catch (Exception e) {
                log.warn("Failed to send achievement notification: {}", e.getMessage());
            }
            
            // Grant achievement rewards
            try {
                grantAchievementRewards(roleId, achievementId);
            } catch (Exception e) {
                log.error("Failed to grant achievement rewards: {}", e.getMessage());
            }
        } else {
            log.debug("Achievement progress: {} - {}/{}", achievementId, progress.getCurrent(), progress.getTarget());
        }

        savePlayerAchievements(roleId, achievements);
    }

    /**
     * Get player achievements from Redis
     */
    @SuppressWarnings("unchecked")
    private Map<String, AchievementProgress> getPlayerAchievements(String roleId) {
        String key = ACHIEVEMENT_KEY_PREFIX + roleId;
        Object achievementsObj = redisTemplate.opsForValue().get(key);
        
        if (achievementsObj instanceof Map) {
            return (Map<String, AchievementProgress>) achievementsObj;
        }
        return new HashMap<>();
    }

    /**
     * Save player achievements to Redis
     */
    private void savePlayerAchievements(String roleId, Map<String, AchievementProgress> achievements) {
        String key = ACHIEVEMENT_KEY_PREFIX + roleId;
        redisTemplate.opsForValue().set(key, achievements, ACHIEVEMENT_TTL_DAYS, TimeUnit.DAYS);
    }

    private boolean isFirstTrialCompletion(String roleId) {
        Map<String, AchievementProgress> achievements = getPlayerAchievements(roleId);
        return !achievements.containsKey("FIRST_TRIAL_VICTORY");
    }

    private boolean isFirstArenaWin(String roleId) {
        Map<String, AchievementProgress> achievements = getPlayerAchievements(roleId);
        return !achievements.containsKey("FIRST_ARENA_VICTORY");
    }

    /**
     * Grant achievement rewards to player (gold, diamonds, items)
     */
    private void grantAchievementRewards(String roleId, String achievementId) {
        AchievementReward reward = ACHIEVEMENT_REWARDS.get(achievementId);
        if (reward == null) {
            log.debug("No reward configured for achievement: {}", achievementId);
            return;
        }

        String eventId = "ACHIEVEMENT_" + achievementId + "_" + System.currentTimeMillis();

        // Grant currency (gold + diamonds)
        if (reward.getGold() > 0 || reward.getDiamond() > 0) {
            try {
                List<WalletDTOs.Change> changes = new ArrayList<>();
                if (reward.getGold() > 0) {
                    changes.add(WalletDTOs.Change.builder()
                            .itemId(1L) // Gold
                            .amount(reward.getGold())
                            .build());
                }
                if (reward.getDiamond() > 0) {
                    changes.add(WalletDTOs.Change.builder()
                            .itemId(3L) // Diamond
                            .amount(reward.getDiamond())
                            .build());
                }

                WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                        .roleId(roleId)
                        .changes(changes)
                        .reason(301) // ACHIEVEMENT_REWARD
                        .build();

                walletClient.addCurrency(roleId, walletReq);
                log.info("Achievement reward granted: roleId={}, achievementId={}, gold={}, diamond={}", 
                        roleId, achievementId, reward.getGold(), reward.getDiamond());
            } catch (Exception e) {
                log.error("Failed to grant currency for achievement {}: {}", achievementId, e.getMessage());
            }
        }

        // Grant items
        if (!reward.getItems().isEmpty()) {
            try {
                List<BagDTOs.GrantItem> items = new ArrayList<>();
                for (ItemReward itemReward : reward.getItems()) {
                    items.add(BagDTOs.GrantItem.builder()
                            .itemId(itemReward.getItemId())
                            .num(itemReward.getQuantity())
                            .bind(0)
                            .build());
                }

                bagClient.grantItems(BagAddItemReq.builder()
                    .userId(Long.parseLong(roleId))
                    .roleId(Long.parseLong(roleId))
                    .idemKey(eventId)
                    .source("ACHIEVEMENT_REWARD")
                    .items(items.stream()
                        .map(item -> BagAddItemReq.Item.builder()
                            .itemId(item.getItemId())
                            .amount(item.getNum())
                            .quality(item.getQuality())
                            .bound(item.getBind() != null && item.getBind() != 0)
                            .bagType(item.getBagType())
                            .build())
                        .toList())
                        .build());
                log.info("Achievement items granted: roleId={}, achievementId={}, items={}", 
                        roleId, achievementId, items.size());
            } catch (Exception e) {
                log.error("Failed to grant items for achievement {}: {}", achievementId, e.getMessage());
            }
        }
    }

    /**
     * Notify player about achievement unlock via Kafka.
     * webSocket-server (or any subscriber) can push SC notification to the client.
     * Topic: task.achievement.unlocked
     * Payload: { roleId, achievementId, description, timestamp }
     */
    private void notifyPlayerAchievement(String roleId, String achievementId, String description) {
        log.info("🔔 [ACHIEVEMENT] Unlocked for {}: {} — {}", roleId, achievementId, description);
        if (kafkaTemplate != null) {
            try {
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("roleId", roleId);
                payload.put("achievementId", achievementId);
                payload.put("description", description);
                payload.put("timestamp", System.currentTimeMillis());
                kafkaTemplate.send(ACHIEVEMENT_UNLOCK_TOPIC, roleId, payload);
                log.debug("[achievement] Kafka event sent: roleId={}, achievementId={}", roleId, achievementId);
            } catch (Exception e) {
                log.warn("[achievement] Failed to publish Kafka event for {}/{}: {}", roleId, achievementId, e.getMessage());
            }
        }
    }

    /**
     * Achievement reward configuration
     */
    @Data
    private static class AchievementReward {
        private final long gold;
        private final long diamond;
        private final List<ItemReward> items;

        public AchievementReward(long gold, long diamond, List<ItemReward> items) {
            this.gold = gold;
            this.diamond = diamond;
            this.items = items != null ? items : Collections.emptyList();
        }
    }

    /**
     * Item reward configuration
     */
    @Data
    private static class ItemReward {
        private final int itemId;
        private final int quantity;

        public ItemReward(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    /**
     * Inner class for tracking achievement progress
     */
    @Data
    public static class AchievementProgress {
        private String id;
        private String description;
        private int current;
        private int target;
        private boolean unlocked;

        public AchievementProgress() {}

        public AchievementProgress(String id, String description, int current, int target, boolean unlocked) {
            this.id = id;
            this.description = description;
            this.current = current;
            this.target = target;
            this.unlocked = unlocked;
        }

        public void increment() {
            current++;
        }

        public boolean isCompleted() {
            return current >= target;
        }
    }
}
