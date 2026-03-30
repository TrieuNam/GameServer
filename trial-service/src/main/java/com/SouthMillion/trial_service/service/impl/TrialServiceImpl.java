package com.SouthMillion.trial_service.service.impl;

import com.SouthMillion.trial_service.client.BagClient;
import com.SouthMillion.trial_service.client.WalletClient;
import com.SouthMillion.trial_service.exception.TrialServiceException;
import com.SouthMillion.trial_service.model.entity.TrialRecord;
import com.SouthMillion.trial_service.repository.TrialRecordRepository;
import com.SouthMillion.trial_service.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrialServiceImpl implements TrialService {
    
    private final TrialRecordRepository trialRecordRepository;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    
    private static final int DEFAULT_DAILY_ATTEMPTS = 3;
    private static final int MAX_STARS = 3;
    
    @Override
    public List<TrialRecord> getAllRecords(Long userId) {
        return trialRecordRepository.findByUserId(userId);
    }
    
    @Override
    public TrialRecord getRecord(Long userId, Integer trialId) {
        return trialRecordRepository.findByUserIdAndTrialId(userId, trialId)
            .orElseThrow(() -> new TrialServiceException(
                "Trial record not found: userId=" + userId + ", trialId=" + trialId,
                "TRIAL_RECORD_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public TrialRecord initializeRecord(Long userId, Integer trialId) {
        log.info("Initializing trial record for user: {}, trialId: {}", userId, trialId);
        
        if (trialRecordRepository.existsByUserIdAndTrialId(userId, trialId)) {
            return getRecord(userId, trialId);
        }
        
        TrialRecord record = new TrialRecord();
        record.setUserId(userId);
        record.setTrialId(trialId);
        record.setCurrentStage(1);
        record.setBestStage(0);
        record.setTotalAttempts(0);
        record.setRemainingAttempts(DEFAULT_DAILY_ATTEMPTS);
        record.setIsInProgress(false);
        record.setCurrentScore(0L);
        record.setBestScore(0L);
        record.setStars(0);
        record.setCompletionTime(null);
        record.setBestTime(null);
        record.setLastResetDate(LocalDateTime.now());
        record.setRewardsClaimed(0L);
        
        TrialRecord saved = trialRecordRepository.save(record);
        log.info("Trial record initialized: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public TrialRecord startTrial(Long userId, Integer trialId) {
        log.info("Starting trial for user: {}, trialId: {}", userId, trialId);
        
        TrialRecord record = trialRecordRepository.findByUserIdAndTrialId(userId, trialId)
            .orElseGet(() -> initializeRecord(userId, trialId));
        
        // Check daily reset
        if (needsDailyReset(record)) {
            resetDailyAttempts(userId, trialId);
            record = getRecord(userId, trialId);
        }
        
        if (record.getIsInProgress()) {
            throw new TrialServiceException("Trial already in progress", "TRIAL_IN_PROGRESS");
        }
        
        if (record.getRemainingAttempts() <= 0) {
            throw new TrialServiceException("No attempts remaining", "NO_ATTEMPTS_REMAINING");
        }
        
        // Check unlock requirements (basic level check, real implementation would check config)
        if (trialId > 1) {
            // For trials beyond first, check if previous trial is completed
            // This is a simplified check
        }
        
        // Consume entry cost (if any)
        int entryCost = 0; // Most trials are free, but some may cost
        if (trialId >= 10) { // High-level trials may have entry cost
            entryCost = (trialId - 9) * 100; // Progressive cost
            consumeGold(userId.toString(), entryCost, "trial_entry");
        }
        
        record.setIsInProgress(true);
        record.setCurrentScore(0L);
        record.setCompletionTime(null);
        record.setRemainingAttempts(record.getRemainingAttempts() - 1);
        record.setTotalAttempts(record.getTotalAttempts() + 1);
        
        TrialRecord saved = trialRecordRepository.save(record);
        log.info("Trial started: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public TrialRecord completeTrial(Long userId, Integer trialId, Long score, Integer stars, Integer completionTime) {
        log.info("Completing trial for user: {}, trialId: {}, score: {}, stars: {}", 
            userId, trialId, score, stars);
        
        TrialRecord record = getRecord(userId, trialId);
        
        if (!record.getIsInProgress()) {
            throw new TrialServiceException("Trial not in progress", "TRIAL_NOT_IN_PROGRESS");
        }
        
        // Validate stars
        if (stars < 0 || stars > MAX_STARS) {
            throw new TrialServiceException("Invalid stars count", "INVALID_STARS");
        }
        
        record.setIsInProgress(false);
        record.setCurrentScore(score);
        record.setStars(Math.max(record.getStars(), stars));
        
        // Update best records
        if (score > record.getBestScore()) {
            record.setBestScore(score);
        }
        
        if (record.getCurrentStage() > record.getBestStage()) {
            record.setBestStage(record.getCurrentStage());
        }
        
        if (completionTime != null) {
            record.setCompletionTime(completionTime);
            if (record.getBestTime() == null || completionTime < record.getBestTime()) {
                record.setBestTime(completionTime);
            }
        }
        
        // Give rewards based on stars earned
        if (stars > 0) {
            long goldReward = 500L * stars * trialId;
            int expReward = 100 * stars * trialId;
            
            addGold(userId.toString(), goldReward, "trial_completion");
            
            // Add trial completion item rewards
            if (stars >= 2) {
                int rewardItemId = 20001; // Trial reward item
                int rewardQuantity = stars;
                addItem(userId.toString(), rewardItemId, rewardQuantity, "trial_completion");
            }
        }
        
        TrialRecord saved = trialRecordRepository.save(record);
        log.info("Trial completed: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public TrialRecord failTrial(Long userId, Integer trialId) {
        log.info("Trial failed for user: {}, trialId: {}", userId, trialId);
        
        TrialRecord record = getRecord(userId, trialId);
        
        if (!record.getIsInProgress()) {
            throw new TrialServiceException("Trial not in progress", "TRIAL_NOT_IN_PROGRESS");
        }
        
        record.setIsInProgress(false);
        record.setCurrentScore(0L);
        record.setCompletionTime(null);
        
        return trialRecordRepository.save(record);
    }
    
    @Override
    @Transactional
    public TrialRecord advanceStage(Long userId, Integer trialId) {
        log.info("Advancing stage for user: {}, trialId: {}", userId, trialId);
        
        TrialRecord record = getRecord(userId, trialId);
        
        if (!record.getIsInProgress()) {
            throw new TrialServiceException("Trial not in progress", "TRIAL_NOT_IN_PROGRESS");
        }
        
        // Validate stage progression (basic validation)
        if (record.getCurrentStage() >= 100) {
            throw new TrialServiceException("Maximum stage reached", "MAX_STAGE_REACHED");
        }
        
        record.setCurrentStage(record.getCurrentStage() + 1);
        
        if (record.getCurrentStage() > record.getBestStage()) {
            record.setBestStage(record.getCurrentStage());
        }
        
        return trialRecordRepository.save(record);
    }
    
    @Override
    @Transactional
    public void resetDailyAttempts(Long userId, Integer trialId) {
        log.info("Resetting daily attempts for user: {}, trialId: {}", userId, trialId);
        
        TrialRecord record = getRecord(userId, trialId);
        record.setRemainingAttempts(DEFAULT_DAILY_ATTEMPTS);
        record.setLastResetDate(LocalDateTime.now());
        
        trialRecordRepository.save(record);
    }
    
    @Override
    @Transactional
    public void resetAllDailyAttempts(Long userId) {
        log.info("Resetting all daily attempts for user: {}", userId);
        
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<TrialRecord> recordsToReset = trialRecordRepository.findByUserIdAndNeedsReset(userId, today);
        
        for (TrialRecord record : recordsToReset) {
            record.setRemainingAttempts(DEFAULT_DAILY_ATTEMPTS);
            record.setLastResetDate(LocalDateTime.now());
        }
        
        trialRecordRepository.saveAll(recordsToReset);
    }
    
    @Override
    @Transactional
    public void resetProgress(Long userId, Integer trialId) {
        log.info("Resetting progress for user: {}, trialId: {}", userId, trialId);
        
        TrialRecord record = getRecord(userId, trialId);
        record.setCurrentStage(1);
        record.setBestStage(0);
        record.setCurrentScore(0L);
        record.setBestScore(0L);
        record.setStars(0);
        record.setCompletionTime(null);
        record.setBestTime(null);
        record.setIsInProgress(false);
        record.setRewardsClaimed(0L);
        
        trialRecordRepository.save(record);
    }
    
    @Override
    @Transactional
    public boolean claimReward(Long userId, Integer trialId, Integer stageId) {
        log.info("Claiming reward for user: {}, trialId: {}, stageId: {}", userId, trialId, stageId);
        
        TrialRecord record = getRecord(userId, trialId);
        
        if (stageId > record.getBestStage()) {
            throw new TrialServiceException("Stage not completed", "STAGE_NOT_COMPLETED");
        }
        
        if (isRewardClaimed(userId, trialId, stageId)) {
            throw new TrialServiceException("Reward already claimed", "REWARD_ALREADY_CLAIMED");
        }
        
        // Set bit flag for claimed reward
        long flag = 1L << (stageId - 1);
        record.setRewardsClaimed(record.getRewardsClaimed() | flag);
        
        // Give stage completion rewards
        long goldReward = 1000L * stageId;
        int itemRewardId = 20002; // Stage reward item
        int itemQuantity = stageId / 5 + 1; // More rewards for higher stages
        
        addGold(userId.toString(), goldReward, "trial_stage_reward");
        addItem(userId.toString(), itemRewardId, itemQuantity, "trial_stage_reward");
        
        trialRecordRepository.save(record);
        return true;
    }
    
    @Override
    public boolean isRewardClaimed(Long userId, Integer trialId, Integer stageId) {
        TrialRecord record = getRecord(userId, trialId);
        long flag = 1L << (stageId - 1);
        return (record.getRewardsClaimed() & flag) != 0;
    }
    
    @Override
    public List<Integer> getClaimedRewards(Long userId, Integer trialId) {
        TrialRecord record = getRecord(userId, trialId);
        List<Integer> claimedStages = new ArrayList<>();
        
        long claimed = record.getRewardsClaimed();
        for (int i = 1; i <= 64; i++) {
            if ((claimed & (1L << (i - 1))) != 0) {
                claimedStages.add(i);
            }
        }
        
        return claimedStages;
    }
    
    @Override
    public List<TrialRecord> getInProgressTrials(Long userId) {
        return trialRecordRepository.findByUserIdAndIsInProgressTrue(userId);
    }
    
    @Override
    public Integer getBestStage(Long userId, Integer trialId) {
        TrialRecord record = getRecord(userId, trialId);
        return record.getBestStage();
    }
    
    @Override
    public Long getBestScore(Long userId, Integer trialId) {
        TrialRecord record = getRecord(userId, trialId);
        return record.getBestScore();
    }

    @Override
    public Integer getBestTime(Long userId, Integer trialId) {
        TrialRecord record = getRecord(userId, trialId);
        return record.getBestTime() != null ? record.getBestTime() : 0;
    }
    
    @Override
    public Long getTotalScore(Long userId) {
        Long total = trialRecordRepository.getTotalScore(userId);
        return total != null ? total : 0L;
    }
    
    @Override
    public boolean canStartTrial(Long userId, Integer trialId) {
        try {
            TrialRecord record = trialRecordRepository.findByUserIdAndTrialId(userId, trialId)
                .orElseGet(() -> initializeRecord(userId, trialId));
            
            if (needsDailyReset(record)) {
                return true; // Will reset on start
            }
            
            if (record.getIsInProgress() || record.getRemainingAttempts() <= 0) {
                return false;
            }
            
            // Check unlock requirements (simplified)
            // Real implementation would check config for unlock conditions
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean hasAttemptsRemaining(Long userId, Integer trialId) {
        try {
            TrialRecord record = getRecord(userId, trialId);
            
            if (needsDailyReset(record)) {
                return true; // Will reset on start
            }
            
            return record.getRemainingAttempts() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean needsDailyReset(TrialRecord record) {
        if (record.getLastResetDate() == null) {
            return true;
        }
        
        LocalDate lastResetDate = record.getLastResetDate().toLocalDate();
        LocalDate today = LocalDate.now();
        
        return lastResetDate.isBefore(today);
    }
    
    /**
     * Consume gold from player wallet via wallet-service
     */
    private void consumeGold(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(1L) // Gold currency
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(3002) // Trial system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new TrialServiceException("Insufficient gold or wallet error", "WALLET_ERROR");
        }
    }
    
    /**
     * Add gold to player wallet via wallet-service
     */
    private void addGold(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(1L) // Gold currency
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(3002) // Trial system reason code
            .build();
        
        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to add gold: {}", e.getMessage());
            throw new TrialServiceException("Wallet error", "WALLET_ERROR");
        }
    }
    
    /**
     * Add item to player bag via bag-service
     */
    private void addItem(String roleId, int itemId, int quantity, String reason) {
        if (quantity <= 0) {
            return;
        }
        
        List<BagDTOs.GrantItem> items = new ArrayList<>();
        BagDTOs.GrantItem item = new BagDTOs.GrantItem();
        item.setItemId(itemId);
        item.setNum(quantity);
        items.add(item);
        
        BagDTOs.GrantReq request = new BagDTOs.GrantReq();
        request.setUserId(roleId);
        request.setRoleId(roleId);
        request.setItems(items);
        request.setEventId(UUID.randomUUID().toString());
        
        try {
            bagClient.grantItems(request);
            log.debug("Added item: itemId={}, quantity={} for {}", itemId, quantity, reason);
        } catch (Exception e) {
            log.error("Failed to add item: {}", e.getMessage());
            throw new TrialServiceException("Bag error", "BAG_ERROR");
        }
    }
}
