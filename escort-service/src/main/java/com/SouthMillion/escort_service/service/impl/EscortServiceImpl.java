package com.SouthMillion.escort_service.service.impl;

import com.SouthMillion.escort_service.client.BagClient;
import com.SouthMillion.escort_service.client.WalletClient;
import com.SouthMillion.escort_service.exception.EscortServiceException;
import com.SouthMillion.escort_service.model.entity.EscortMission;
import com.SouthMillion.escort_service.model.entity.EscortStats;
import com.SouthMillion.escort_service.repository.EscortMissionRepository;
import com.SouthMillion.escort_service.repository.EscortStatsRepository;
import com.SouthMillion.escort_service.service.EscortService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscortServiceImpl implements EscortService {
    
    private final EscortMissionRepository missionRepository;
    private final EscortStatsRepository statsRepository;
    private final WalletClient walletClient;
    private final BagClient bagClient;
    private final Random random = new Random();
    
    private static final int STATUS_AVAILABLE = 0;
    private static final int STATUS_IN_PROGRESS = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_FAILED = 3;
    private static final int STATUS_EXPIRED = 4;
    
    private static final int DEFAULT_DAILY_LIMIT = 10;
    private static final int DEFAULT_FREE_REFRESHES = 3;
    private static final int MISSION_DURATION_HOURS = 2;
    private static final int MAX_SHIP_LEVEL = 5;
    
    @Override
    public List<EscortMission> getAllMissions(String userId) {
        return missionRepository.findByUserId(uid(userId));
    }
    
    @Override
    public EscortMission getMission(String userId, Long missionId) {
        return missionRepository.findByUserIdAndId(uid(userId), missionId)
            .orElseThrow(() -> new EscortServiceException(
                "Mission not found: userId=" + userId + ", missionId=" + missionId,
                "MISSION_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public EscortMission generateMission(String userId, Integer quality) {
        log.info("Generating escort mission for user: {}, quality: {}", userId, quality);
        
        if (quality < 1 || quality > 5) {
            throw new EscortServiceException("Invalid quality tier", "INVALID_QUALITY");
        }
        
        EscortStats stats = getOrCreateStats(userId);
        
        EscortMission mission = new EscortMission();
        mission.setUserId(uid(userId));
        // Select mission template based on quality tier (20 templates per quality level)
        int missionBase = (quality - 1) * 20 + 1;  // quality 1 → 1-20, quality 2 → 21-40, ...
        mission.setMissionId(missionBase + random.nextInt(20));
        mission.setQuality(quality);
        mission.setStatus(STATUS_AVAILABLE);
        mission.setStartTime(null);
        mission.setExpiryTime(null);
        mission.setCompletionTime(null);
        mission.setDistance(1000 * quality); // Higher quality = longer distance
        mission.setProgress(0);
        mission.setAttacksSurvived(0);
        mission.setRewardGold((long) (100 * quality * quality));
        mission.setRewardExp((long) (50 * quality * quality));
        mission.setBonusMultiplier(1.0 + (quality * 0.2));
        mission.setIsRewardClaimed(false);
        mission.setSourceLocation(random.nextInt(10) + 1);
        mission.setDestinationLocation(random.nextInt(10) + 11);
        mission.setCargoType(random.nextInt(5) + 1);
        
        EscortMission saved = missionRepository.save(mission);
        log.info("Mission generated: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public EscortMission startMission(String userId, Long missionId) {
        log.info("Starting escort mission for user: {}, missionId: {}", userId, missionId);
        
        EscortStats stats = getOrCreateStats(userId);
        
        // Check daily reset
        if (needsDailyReset(stats)) {
            resetDailyStats(userId);
            stats = getStats(userId);
        }
        
        if (stats.getDailyCompleted() >= stats.getDailyLimit()) {
            throw new EscortServiceException("Daily mission limit reached", "DAILY_LIMIT_REACHED");
        }
        
        if (hasActiveMission(userId)) {
            throw new EscortServiceException("Already have active mission", "ACTIVE_MISSION_EXISTS");
        }
        
        EscortMission mission = getMission(userId, missionId);
        
        if (mission.getStatus() != STATUS_AVAILABLE) {
            throw new EscortServiceException("Mission not available", "MISSION_NOT_AVAILABLE");
        }
        
        // Check unlock requirements (basic level check)
        // Higher quality missions require higher level
        // In real implementation, check via role-service
        
        // Check and consume entry cost for high quality missions
        if (mission.getQuality() >= 4) { // Purple or Orange
            long entryCost = mission.getQuality() * 500L;
            consumeGold(userId, entryCost, "escort_entry");
        }
        
        mission.setStatus(STATUS_IN_PROGRESS);
        mission.setStartTime(LocalDateTime.now());
        mission.setExpiryTime(LocalDateTime.now().plusHours(MISSION_DURATION_HOURS));
        mission.setProgress(0);
        
        EscortMission saved = missionRepository.save(mission);
        log.info("Mission started: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public EscortMission updateProgress(String userId, Long missionId, Integer progressIncrement) {
        log.info("Updating mission progress for user: {}, missionId: {}, increment: {}", 
            userId, missionId, progressIncrement);
        
        EscortMission mission = getMission(userId, missionId);
        
        if (mission.getStatus() != STATUS_IN_PROGRESS) {
            throw new EscortServiceException("Mission not in progress", "MISSION_NOT_IN_PROGRESS");
        }
        
        checkExpiredMissions(userId);
        mission = getMission(userId, missionId);
        
        if (mission.getStatus() == STATUS_EXPIRED) {
            throw new EscortServiceException("Mission expired", "MISSION_EXPIRED");
        }
        
        int newProgress = Math.min(mission.getProgress() + progressIncrement, mission.getDistance());
        mission.setProgress(newProgress);
        
        // Random attack event
        if (random.nextDouble() < 0.1) { // 10% chance per update
            mission.setAttacksSurvived(mission.getAttacksSurvived() + 1);
            log.info("Mission attacked! Total attacks survived: {}", mission.getAttacksSurvived());
        }
        
        return missionRepository.save(mission);
    }
    
    @Override
    @Transactional
    public EscortMission completeMission(String userId, Long missionId) {
        log.info("Completing escort mission for user: {}, missionId: {}", userId, missionId);
        
        EscortMission mission = getMission(userId, missionId);
        
        if (mission.getStatus() != STATUS_IN_PROGRESS) {
            throw new EscortServiceException("Mission not in progress", "MISSION_NOT_IN_PROGRESS");
        }
        
        if (mission.getProgress() < mission.getDistance()) {
            throw new EscortServiceException("Mission not completed yet", "MISSION_NOT_COMPLETED");
        }
        
        mission.setStatus(STATUS_COMPLETED);
        mission.setCompletionTime(LocalDateTime.now());
        
        // Update stats
        EscortStats stats = getStats(userId);
        stats.setTotalCompleted(stats.getTotalCompleted() + 1);
        stats.setDailyCompleted(stats.getDailyCompleted() + 1);
        stats.setTotalDistance(stats.getTotalDistance() + mission.getDistance());
        
        if (mission.getQuality() > stats.getBestQuality()) {
            stats.setBestQuality(mission.getQuality());
        }
        
        if (mission.getAttacksSurvived() == 0) {
            stats.setPerfectCompletions(stats.getPerfectCompletions() + 1);
            mission.setBonusMultiplier(mission.getBonusMultiplier() * 1.5); // Perfect bonus
        }
        
        statsRepository.save(stats);
        
        EscortMission saved = missionRepository.save(mission);
        log.info("Mission completed: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public EscortMission failMission(String userId, Long missionId) {
        log.info("Failing escort mission for user: {}, missionId: {}", userId, missionId);
        
        EscortMission mission = getMission(userId, missionId);
        
        if (mission.getStatus() != STATUS_IN_PROGRESS) {
            throw new EscortServiceException("Mission not in progress", "MISSION_NOT_IN_PROGRESS");
        }
        
        mission.setStatus(STATUS_FAILED);
        mission.setCompletionTime(LocalDateTime.now());
        
        // Update stats
        EscortStats stats = getStats(userId);
        stats.setTotalFailed(stats.getTotalFailed() + 1);
        statsRepository.save(stats);
        
        return missionRepository.save(mission);
    }
    
    @Override
    @Transactional
    public void cancelMission(String userId, Long missionId) {
        log.info("Canceling escort mission for user: {}, missionId: {}", userId, missionId);
        
        EscortMission mission = getMission(userId, missionId);
        
        if (mission.getStatus() != STATUS_AVAILABLE && mission.getStatus() != STATUS_IN_PROGRESS) {
            throw new EscortServiceException("Cannot cancel mission in current status", "CANNOT_CANCEL");
        }
        
        missionRepository.delete(mission);
    }
    
    @Override
    @Transactional
    public EscortMission claimReward(String userId, Long missionId) {
        log.info("Claiming reward for user: {}, missionId: {}", userId, missionId);
        
        EscortMission mission = getMission(userId, missionId);
        
        if (mission.getStatus() != STATUS_COMPLETED) {
            throw new EscortServiceException("Mission not completed", "MISSION_NOT_COMPLETED");
        }
        
        if (mission.getIsRewardClaimed()) {
            throw new EscortServiceException("Reward already claimed", "REWARD_ALREADY_CLAIMED");
        }
        
        long finalGold = (long) (mission.getRewardGold() * mission.getBonusMultiplier());
        long finalExp = (long) (mission.getRewardExp() * mission.getBonusMultiplier());
        
        // Give rewards via wallet-service
        addGold(userId, finalGold);
        addExp(userId, finalExp);
        log.info("Rewards granted - Gold: {}, Exp: {}", finalGold, finalExp);
        
        mission.setIsRewardClaimed(true);
        
        // Update stats
        EscortStats stats = getStats(userId);
        stats.setTotalGoldEarned(stats.getTotalGoldEarned() + finalGold);
        stats.setTotalExpEarned(stats.getTotalExpEarned() + finalExp);
        statsRepository.save(stats);
        
        return missionRepository.save(mission);
    }
    
    @Override
    @Transactional
    public List<EscortMission> refreshMissions(String userId) {
        log.info("Refreshing missions for user: {}", userId);
        
        EscortStats stats = getOrCreateStats(userId);
        
        // Check daily reset
        if (needsDailyReset(stats)) {
            resetDailyStats(userId);
            stats = getStats(userId);
        }
        
        if (stats.getFreeRefreshes() <= 0) {
            // Try to consume paid refresh item (itemId 5001)
            try {
                consumeMaterial(userId, 5001, 1);
                log.info("Used paid refresh item for user: {}", userId);
            } catch (Exception e) {
                throw new EscortServiceException("No free refreshes and no refresh items", "NO_REFRESHES_AVAILABLE");
            }
        } else {
            stats.setFreeRefreshes(stats.getFreeRefreshes() - 1);
        }
        
        // Delete available missions
        List<EscortMission> availableMissions = missionRepository.findByUserIdAndStatus(uid(userId), STATUS_AVAILABLE);
        missionRepository.deleteAll(availableMissions);
        
        // Generate new missions
        List<EscortMission> newMissions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int quality = generateRandomQuality();
            EscortMission mission = generateMission(userId, quality);
            newMissions.add(mission);
        }
        
        stats.setTotalRefreshesToday(stats.getTotalRefreshesToday() + 1);
        statsRepository.save(stats);
        
        log.info("Generated {} new missions", newMissions.size());
        return newMissions;
    }
    
    @Override
    @Transactional
    public void checkExpiredMissions(String userId) {
        List<EscortMission> expiredMissions = missionRepository.findExpiredMissions(uid(userId), LocalDateTime.now());
        
        for (EscortMission mission : expiredMissions) {
            mission.setStatus(STATUS_EXPIRED);
            log.info("Mission expired: {}", mission.getId());
        }
        
        if (!expiredMissions.isEmpty()) {
            missionRepository.saveAll(expiredMissions);
        }
    }
    
    @Override
    public List<EscortMission> getActiveMissions(String userId) {
        return missionRepository.findByUserIdAndStatus(uid(userId), STATUS_IN_PROGRESS);
    }
    
    @Override
    public List<EscortMission> getCompletedMissions(String userId) {
        return missionRepository.findByUserIdAndStatus(uid(userId), STATUS_COMPLETED);
    }
    
    @Override
    public List<EscortMission> getUnclaimedRewards(String userId) {
        return missionRepository.findUnclaimedRewards(uid(userId));
    }
    
    @Override
    public EscortStats getStats(String userId) {
        return statsRepository.findByUserId(uid(userId))
            .orElseThrow(() -> new EscortServiceException(
                "Stats not found for user: " + userId,
                "STATS_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public EscortStats initializeStats(String userId) {
        log.info("Initializing escort stats for user: {}", userId);
        
        if (statsRepository.existsByUserId(uid(userId))) {
            return getStats(userId);
        }
        
        EscortStats stats = new EscortStats();
        stats.setUserId(uid(userId));
        stats.setTotalCompleted(0);
        stats.setTotalFailed(0);
        stats.setDailyCompleted(0);
        stats.setDailyLimit(DEFAULT_DAILY_LIMIT);
        stats.setFreeRefreshes(DEFAULT_FREE_REFRESHES);
        stats.setTotalRefreshesToday(0);
        stats.setLastResetDate(LocalDateTime.now());
        stats.setBestQuality(0);
        stats.setPerfectCompletions(0);
        stats.setTotalDistance(0L);
        stats.setTotalGoldEarned(0L);
        stats.setTotalExpEarned(0L);
        // ── PB_SCEscortRoleInfo (9622) proto-specific fields (explicit init) ──
        stats.setCurrentShipLevel(1);
        stats.setEscortCount(0);
        stats.setInterceptCount(0);
        stats.setHelpCount(0);
        stats.setRewardsClaimed(0);
        
        return statsRepository.save(stats);
    }

    @Override
    @Transactional
    public EscortStats saveStats(EscortStats stats) {
        return statsRepository.save(stats);
    }

    /**
     * Auto-complete all IN_PROGRESS missions so the client can claim rewards
     * without a separate completion step.  Sets progress = distance then
     * delegates to completeMission()
     */
    @Override
    @Transactional
    public void autoCompleteMissions(String userId) {
        List<EscortMission> inProgress = missionRepository.findByUserIdAndStatus(uid(userId), STATUS_IN_PROGRESS);
        for (EscortMission mission : inProgress) {
            // Force progress to distance so completeMission() accepts it
            if (mission.getProgress() < mission.getDistance()) {
                mission.setProgress(mission.getDistance());
                missionRepository.save(mission);
            }
            try {
                completeMission(userId, mission.getId());
            } catch (Exception ex) {
                log.warn("[Escort] autoCompleteMissions: skip missionId={} reason={}", mission.getId(), ex.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public EscortStats recordIntercept(String userId) {
        EscortStats stats = getOrCreateStats(userId);
        int current = stats.getInterceptCount() != null ? stats.getInterceptCount() : 0;
        stats.setInterceptCount(current + 1);
        EscortStats saved = statsRepository.save(stats);
        log.debug("[Escort] recordIntercept userId={} interceptCount={}", userId, saved.getInterceptCount());
        return saved;
    }

    @Override
    @Transactional
    public EscortStats recordHelp(String userId) {
        EscortStats stats = getOrCreateStats(userId);
        int current = stats.getHelpCount() != null ? stats.getHelpCount() : 0;
        stats.setHelpCount(current + 1);
        EscortStats saved = statsRepository.save(stats);
        log.debug("[Escort] recordHelp userId={} helpCount={}", userId, saved.getHelpCount());
        return saved;
    }

    @Override
    @Transactional
    public int upgradeShipLevel(String userId) {
        EscortStats stats = getOrCreateStats(userId);
        int current = stats.getCurrentShipLevel() != null ? stats.getCurrentShipLevel() : 0;
        if (current >= MAX_SHIP_LEVEL) {
            return current;
        }
        stats.setCurrentShipLevel(current + 1);
        EscortStats saved = statsRepository.save(stats);
        return saved.getCurrentShipLevel() != null ? saved.getCurrentShipLevel() : current + 1;
    }

    @Override
    @Transactional
    public void resetDailyStats(String userId) {
        log.info("Resetting daily stats for user: {}", userId);
        
        EscortStats stats = getStats(userId);
        stats.setDailyCompleted(0);
        stats.setFreeRefreshes(DEFAULT_FREE_REFRESHES);
        stats.setTotalRefreshesToday(0);
        stats.setLastResetDate(LocalDateTime.now());
        
        statsRepository.save(stats);
    }
    
    @Override
    public boolean canStartMission(String userId) {
        try {
            EscortStats stats = getOrCreateStats(userId);
            
            if (needsDailyReset(stats)) {
                return true; // Will reset on start
            }
            
            return stats.getDailyCompleted() < stats.getDailyLimit() && !hasActiveMission(userId);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean canRefresh(String userId) {
        try {
            EscortStats stats = getOrCreateStats(userId);
            
            if (needsDailyReset(stats)) {
                return true; // Will reset on refresh
            }
            
            // Check free refreshes or paid refresh items available
            // In real implementation, check bag-service for item 5001
            return stats.getFreeRefreshes() > 0; // Simplified
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean hasActiveMission(String userId) {
        return missionRepository.countByUserIdAndStatus(uid(userId), STATUS_IN_PROGRESS) > 0;
    }
    
    private EscortStats getOrCreateStats(String userId) {
        return statsRepository.findByUserId(uid(userId))
            .orElseGet(() -> initializeStats(userId));
    }
    
    private boolean needsDailyReset(EscortStats stats) {
        if (stats.getLastResetDate() == null) {
            return true;
        }
        
        LocalDate lastResetDate = stats.getLastResetDate().toLocalDate();
        LocalDate today = LocalDate.now();
        
        return lastResetDate.isBefore(today);
    }
    
    private int generateRandomQuality() {
        // Weighted random: 50% white, 30% green, 15% blue, 4% purple, 1% orange
        double rand = random.nextDouble();
        if (rand < 0.50) return 1; // White
        if (rand < 0.80) return 2; // Green
        if (rand < 0.95) return 3; // Blue
        if (rand < 0.99) return 4; // Purple
        return 5; // Orange
    }
    
    /** Convert String userId (from API/controller) to Long for repository calls. */
    private static Long uid(String s) { return Long.parseLong(s); }

    /**
     * Consume gold from player wallet
     */
    private void consumeGold(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(1L) // Gold
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(3001) // Escort system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new EscortServiceException("Insufficient gold: " + e.getMessage(), "WALLET_ERROR");
        }
    }
    
    /**
     * Add gold to player wallet
     */
    private void addGold(String playerId, long amount) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(1L) // Gold
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(3002) // Escort reward reason code
            .build();
        
        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} gold", amount);
        } catch (Exception e) {
            log.error("Failed to add gold: {}", e.getMessage());
            throw new EscortServiceException("Failed to grant gold: " + e.getMessage(), "WALLET_ERROR");
        }
    }
    
    /**
     * Add exp to player wallet
     */
    private void addExp(String playerId, long amount) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(2L) // Experience
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(3003) // Escort exp reason code
            .build();
        
        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} exp", amount);
        } catch (Exception e) {
            log.error("Failed to add exp: {}", e.getMessage());
            throw new EscortServiceException("Failed to grant exp: " + e.getMessage(), "WALLET_ERROR");
        }
    }
    
    /**
     * Consume material from player bag
     */
    private void consumeMaterial(String roleId, int itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        
        BagDTOs.UseItemReq request = new BagDTOs.UseItemReq();
        request.setItemId(itemId);
        request.setNum(quantity);
        
        try {
            bagClient.useItem(roleId, request);
            log.debug("Consumed material: itemId={}, quantity={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to consume material: {}", e.getMessage());
            throw new EscortServiceException("Insufficient materials: " + e.getMessage(), "BAG_ERROR");
        }
    }

    @Override
    @Transactional
    public EscortMission robEscort(String userId, String victimId) {
        log.info("[escort] robEscort: attacker={}, victim={}", userId, victimId);
        // Find victim's first active (IN_PROGRESS) mission
        List<EscortMission> victimMissions = missionRepository.findByUserId(uid(victimId)).stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == STATUS_IN_PROGRESS)
                .toList();
        if (victimMissions.isEmpty()) {
            log.info("[escort] No active mission for victim={}", victimId);
            return null;
        }
        EscortMission target = victimMissions.get(0);
        target.setStatus(STATUS_FAILED);
        target.setCompletionTime(LocalDateTime.now());
        EscortMission saved = missionRepository.save(target);
        // Update attacker intercept stats
        recordIntercept(userId);
        log.info("[escort] Intercepted missionId={} of victim={}", target.getId(), victimId);
        return saved;
    }

    @Override
    @Transactional
    public EscortMission speedupEscort(String userId, Long missionId) {
        log.info("[escort] speedupEscort: userId={}, missionId={}", userId, missionId);
        EscortMission mission = missionRepository.findByUserIdAndId(uid(userId), missionId)
                .orElseThrow(() -> new EscortServiceException(
                        "Mission not found: " + missionId, "MISSION_NOT_FOUND"));
        if (mission.getStatus() == null || mission.getStatus() != STATUS_IN_PROGRESS) {
            throw new EscortServiceException("Mission is not in progress", "INVALID_STATE");
        }
        // Force progress = distance → mission can be claimed immediately
        mission.setProgress(mission.getDistance() != null ? mission.getDistance() : 1000);
        EscortMission saved = missionRepository.save(mission);
        log.info("[escort] Mission {} sped up — progress={}/{}", missionId, saved.getProgress(), saved.getDistance());
        return saved;
    }
}
