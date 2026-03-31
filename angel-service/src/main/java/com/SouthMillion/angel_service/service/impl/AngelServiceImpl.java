package com.SouthMillion.angel_service.service.impl;

import com.SouthMillion.angel_service.client.BagClient;
import com.SouthMillion.angel_service.client.WalletClient;
import com.SouthMillion.angel_service.config.AngelConfigHolder;
import com.SouthMillion.angel_service.event.AngelEventPublisher;
import com.SouthMillion.angel_service.exception.AngelServiceException;
import com.SouthMillion.angel_service.model.entity.Angel;
import com.SouthMillion.angel_service.repository.AngelRepository;
import com.SouthMillion.angel_service.service.AngelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Angel Service Implementation
 * C++ Source: gameworld/other/angel/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AngelServiceImpl implements AngelService {
    
    private final AngelRepository angelRepository;
    private final AngelConfigHolder configHolder;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    private final AngelEventPublisher eventPublisher;
    
    // Constants
    private static final int MAX_ANGEL_LEVEL = 100;
    private static final int MAX_ANGEL_GRADE = 10;
    private static final int MAX_STAR_LEVEL = 12;
    private static final int MAX_SKILL_LEVEL = 10;
    private static final int MAX_EVOLUTION_STAGE = 5;
    private static final int SKILL_SLOTS = 4;
    private static final long RENAME_GOLD_COST = 10_000L;
    private static final int MAX_NAME_LENGTH = 12;
    
    @Override
    public List<Angel> getAllAngels(Long userId) {
        log.debug("Getting all angels for user: {}", userId);
        return angelRepository.findByUserId(userId);
    }
    
    @Override
    public Angel getAngel(Long userId, Integer angelIndex) {
        log.debug("Getting angel for user: {}, index: {}", userId, angelIndex);
        return angelRepository.findByUserIdAndAngelIndex(userId, angelIndex)
            .orElseThrow(() -> new AngelServiceException(
                "Angel not found: userId=" + userId + ", index=" + angelIndex,
                "ANGEL_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Angel unlockAngel(Long userId, Integer angelId) {
        log.info("Unlocking angel for user: {}, angelId: {}", userId, angelId);
        
        // Check if angel already exists
        if (angelRepository.findByUserIdAndAngelId(userId, angelId).isPresent()) {
            throw new AngelServiceException("Angel already unlocked", "ANGEL_ALREADY_EXISTS");
        }
        
        // Check unlock requirements and consume materials
        AngelConfigHolder.UnlockCost cost = configHolder.getUnlockCost(angelId);
        consumeGold(userId.toString(), cost.getGoldCost(), "angel_unlock");
        consumeMaterial(userId.toString(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        List<Angel> existingAngels = angelRepository.findByUserId(userId);
        int nextIndex = existingAngels.size();
        
        Angel angel = new Angel();
        angel.setUserId(userId);
        angel.setAngelIndex(nextIndex);
        angel.setAngelId(angelId);
        angel.setLevel(1);
        angel.setGrade(1);
        angel.setExp(0L);
        angel.setIsActive(true);
        angel.setIsEquipped(false);
        angel.setStarLevel(0);
        angel.setSkill1Level(0);
        angel.setSkill2Level(0);
        angel.setSkill3Level(0);
        angel.setSkill4Level(0);
        angel.setEvolutionStage(0);
        angel.setBlessingPoints(0L);
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel unlocked: {}", saved);
        
        // Publish event
        eventPublisher.publishAngelUnlocked(
            userId, angelId, nextIndex, 1, 1
        );
        
        return saved;
    }
    
    @Override
    @Transactional
    public Angel levelUpAngel(Long userId, Integer angelIndex) {
        log.info("Leveling up angel for user: {}, index: {}", userId, angelIndex);
        
        Angel angel = getAngel(userId, angelIndex);
        
        if (!angel.getIsActive()) {
            throw new AngelServiceException("Angel not activated", "ANGEL_NOT_ACTIVE");
        }
        
        if (angel.getLevel() >= MAX_ANGEL_LEVEL) {
            throw new AngelServiceException("Angel already at max level", "MAX_LEVEL_REACHED");
        }
        
        // Get level up cost and consume materials
        AngelConfigHolder.LevelUpCost cost = configHolder.getLevelUpCost(angel.getLevel());
        consumeGold(userId.toString(), cost.getGoldCost(), "angel_levelup");
        consumeMaterial(userId.toString(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        Integer oldLevel = angel.getLevel();
        angel.setLevel(angel.getLevel() + 1);
        angel.setExp(0L);
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel leveled up to: {}", saved.getLevel());
        
        // Publish event
        eventPublisher.publishAngelLevelUp(
            userId, angel.getAngelId(), angelIndex, oldLevel, saved.getLevel(), 1000L
        );
        
        return saved;
    }
    
    @Override
    @Transactional
    public Angel gradeUpAngel(Long userId, Integer angelIndex) {
        log.info("Upgrading angel grade for user: {}, index: {}", userId, angelIndex);
        
        Angel angel = getAngel(userId, angelIndex);
        
        if (angel.getGrade() >= MAX_ANGEL_GRADE) {
            throw new AngelServiceException("Angel already at max grade", "MAX_GRADE_REACHED");
        }
        
        // Get grade up cost and consume materials
        AngelConfigHolder.GradeUpCost cost = configHolder.getGradeUpCost(angel.getGrade());
        consumeGold(userId.toString(), cost.getGoldCost(), "angel_gradeup");
        consumeMaterial(userId.toString(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        angel.setGrade(angel.getGrade() + 1);
        angel.setLevel(1);
        angel.setExp(0L);
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel grade upgraded to: {}", saved.getGrade());
        return saved;
    }
    
    @Override
    @Transactional
    public void equipAngel(Long userId, Integer angelIndex) {
        log.info("Equipping angel for user: {}, index: {}", userId, angelIndex);
        
        Angel angel = getAngel(userId, angelIndex);
        
        if (!angel.getIsActive()) {
            throw new AngelServiceException("Cannot equip inactive angel", "ANGEL_NOT_ACTIVE");
        }
        
        // Unequip all other angels
        angelRepository.unequipAllAngels(userId);
        
        angel.setIsEquipped(true);
        angelRepository.save(angel);
        
        // Recalculate role capability with angel stats
        Long angelPower = calculateAngelPower(angel);
        log.info("Angel equipped: {}, adding power: {}", angelIndex, angelPower);
        // Note: Role-service integration for capability update would be done via RoleClient
    }
    
    @Override
    @Transactional
    public void unequipAngel(Long userId) {
        log.info("Unequipping angel for user: {}", userId);
        angelRepository.unequipAllAngels(userId);
        // Recalculate role capability without angel stats
        log.info("Angel unequipped for user: {}, removing angel power from capability", userId);
        // Note: Role-service integration for capability update would be done via RoleClient
    }
    
    @Override
    @Transactional
    public Angel upgradeStarLevel(Long userId, Integer angelIndex) {
        log.info("Upgrading angel star level for user: {}, index: {}", userId, angelIndex);
        
        Angel angel = getAngel(userId, angelIndex);
        
        if (angel.getStarLevel() >= MAX_STAR_LEVEL) {
            throw new AngelServiceException("Angel already at max star level", "MAX_STAR_REACHED");
        }
        
        // Get star upgrade cost and consume materials
        AngelConfigHolder.StarUpCost cost = configHolder.getStarUpCost(angel.getStarLevel());
        consumeGold(userId.toString(), cost.getGoldCost(), "angel_star_upgrade");
        consumeMaterial(userId.toString(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        angel.setStarLevel(angel.getStarLevel() + 1);
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel star level upgraded to: {}", saved.getStarLevel());
        return saved;
    }
    
    @Override
    @Transactional
    public Angel upgradeSkill(Long userId, Integer angelIndex, Integer skillSlot) {
        log.info("Upgrading angel skill for user: {}, index: {}, slot: {}", 
            userId, angelIndex, skillSlot);
        
        if (skillSlot < 1 || skillSlot > SKILL_SLOTS) {
            throw new AngelServiceException("Invalid skill slot: " + skillSlot, "INVALID_SKILL_SLOT");
        }
        
        Angel angel = getAngel(userId, angelIndex);
        
        Integer currentLevel = switch (skillSlot) {
            case 1 -> angel.getSkill1Level();
            case 2 -> angel.getSkill2Level();
            case 3 -> angel.getSkill3Level();
            case 4 -> angel.getSkill4Level();
            default -> 0;
        };
        
        if (currentLevel >= MAX_SKILL_LEVEL) {
            throw new AngelServiceException("Skill already at max level", "MAX_SKILL_LEVEL_REACHED");
        }
        
        // Get skill upgrade cost and consume materials
        AngelConfigHolder.SkillUpCost cost = configHolder.getSkillUpCost(currentLevel);
        consumeGold(userId.toString(), cost.getGoldCost(), "angel_skill_upgrade");
        consumeMaterial(userId.toString(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        switch (skillSlot) {
            case 1 -> angel.setSkill1Level(currentLevel + 1);
            case 2 -> angel.setSkill2Level(currentLevel + 1);
            case 3 -> angel.setSkill3Level(currentLevel + 1);
            case 4 -> angel.setSkill4Level(currentLevel + 1);
        }
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel skill {} upgraded to level: {}", skillSlot, currentLevel + 1);
        return saved;
    }
    
    @Override
    @Transactional
    public void setAppearance(Long userId, Integer angelIndex, Integer appearanceId) {
        log.info("Setting angel appearance for user: {}, index: {}, appearance: {}", 
            userId, angelIndex, appearanceId);
        
        Angel angel = getAngel(userId, angelIndex);
        
        // Validate appearance ID from config
        if (appearanceId != null && appearanceId < 0) {
            throw new AngelServiceException("Invalid appearance ID: " + appearanceId, "INVALID_APPEARANCE");
        }
        // Note: Full validation would check against AngelConfigHolder.getAngelConfig()
        
        angel.setAppearanceId(appearanceId);
        angelRepository.save(angel);
    }
    
    @Override
    @Transactional
    public Angel upgradeAppearance(Long userId, Integer angelIndex, Integer targetLevel) {
        log.info("Upgrading angel appearance - userId: {}, angelIndex: {}, targetLevel: {}", 
                userId, angelIndex, targetLevel);
        
        Angel angel = getAngel(userId, angelIndex);
        
        // Validate angel is active
        if (!angel.getIsActive()) {
            throw new AngelServiceException("Angel not unlocked", "NOT_UNLOCKED");
        }
        
        // Validate appearance is set
        if (angel.getAppearanceId() == null || angel.getAppearanceId() <= 0) {
            throw new AngelServiceException("No appearance set", "NO_APPEARANCE");
        }
        
        int currentLevel = angel.getAppearanceLevel();
        
        // Validate target level
        if (targetLevel <= currentLevel) {
            throw new AngelServiceException("Target level must be higher than current", "INVALID_TARGET");
        }
        
        int maxAppearanceLevel = 10; // Max appearance level
        if (targetLevel > maxAppearanceLevel) {
            throw new AngelServiceException("Target exceeds max level: " + maxAppearanceLevel, "MAX_LEVEL");
        }
        
        // Calculate total cost from current to target
        long totalCost = 0;
        for (int level = currentLevel; level < targetLevel; level++) {
            totalCost += 5000L * (level + 1); // Increasing cost per level
        }
        
        // Consume currency (gold)
        consumeGold(String.valueOf(userId), totalCost, "Angel appearance upgrade");
        
        // Set new level
        angel.setAppearanceLevel(targetLevel);
        
        log.info("Upgraded angel appearance - userId: {}, angelIndex: {}, {} -> {}, cost: {}", 
                userId, angelIndex, currentLevel, targetLevel, totalCost);
        
        return angelRepository.save(angel);
    }
    
    @Override
    @Transactional
    public Angel evolveAngel(Long userId, Integer angelIndex) {
        log.info("Evolving angel for user: {}, index: {}", userId, angelIndex);
        
        Angel angel = getAngel(userId, angelIndex);
        
        if (angel.getEvolutionStage() >= MAX_EVOLUTION_STAGE) {
            throw new AngelServiceException("Angel already at max evolution", "MAX_EVOLUTION_REACHED");
        }
        
        // Get evolution cost and consume materials
        AngelConfigHolder.EvolutionCost cost = configHolder.getEvolutionCost(angel.getEvolutionStage());
        consumeGold(userId.toString(), cost.getGoldCost(), "angel_evolution");
        consumeMaterial(userId.toString(), cost.getMaterialItemId(), cost.getMaterialQuantity());
        
        Integer oldStage = angel.getEvolutionStage();
        angel.setEvolutionStage(angel.getEvolutionStage() + 1);
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel evolved to stage: {}", saved.getEvolutionStage());
        
        // Publish event
        eventPublisher.publishAngelEvolved(
            userId, angel.getAngelId(), angelIndex, oldStage, saved.getEvolutionStage(), angel.getLevel()
        );
        
        return saved;
    }
    
    @Override
    @Transactional
    public Angel addBlessing(Long userId, Integer angelIndex, Long points) {
        log.info("Adding blessing to angel for user: {}, index: {}, points: {}", 
            userId, angelIndex, points);
        
        Angel angel = getAngel(userId, angelIndex);
        angel.setBlessingPoints(angel.getBlessingPoints() + points);
        
        Angel saved = angelRepository.save(angel);
        log.info("Angel blessing points: {}", saved.getBlessingPoints());
        return saved;
    }
    
    @Override
    public Long calculateAngelPower(Angel angel) {
        if (!angel.getIsActive()) {
            return 0L;
        }
        
        // Calculate angel power using config-based formula
        // Note: In production, formula coefficients would come from AngelConfigHolder
        long basePower = angel.getLevel() * 150L;
        long gradePower = angel.getGrade() * 800L;
        long starPower = angel.getStarLevel() * 300L;
        long evolutionPower = angel.getEvolutionStage() * 1000L;
        long skillPower = (angel.getSkill1Level() + angel.getSkill2Level() + 
                          angel.getSkill3Level() + angel.getSkill4Level()) * 100L;
        long blessingPower = angel.getBlessingPoints() / 10;
        
        long totalPower = basePower + gradePower + starPower + evolutionPower + skillPower + blessingPower;
        
        log.debug("Angel power calculated: {}", totalPower);
        return totalPower;
    }
    
    @Override
    public boolean canLevelUp(Long userId, Integer angelIndex) {
        try {
            Angel angel = getAngel(userId, angelIndex);
            // Check if can level up (material check would be done via bag-service)
            boolean hasLevel = angel.getLevel() < MAX_ANGEL_LEVEL;
            // Note: Full implementation: bagClient.checkItem(userId, materialItemId, quantity)
            return angel.getIsActive() && hasLevel;
        } catch (AngelServiceException e) {
            return false;
        }
    }
    
    @Override
    public boolean canGradeUp(Long userId, Integer angelIndex) {
        try {
            Angel angel = getAngel(userId, angelIndex);
            // Check grade up requirements (would validate via config)
            boolean hasGrade = angel.getGrade() < MAX_ANGEL_GRADE;
            // Note: Full implementation would check level requirement and materials
            return angel.getIsActive() && hasGrade;
        } catch (AngelServiceException e) {
            return false;
        }
    }
    
    @Override
    public boolean canEvolve(Long userId, Integer angelIndex) {
        try {
            Angel angel = getAngel(userId, angelIndex);
            // Check evolution requirements (would validate via config)
            boolean hasEvolution = angel.getEvolutionStage() < MAX_EVOLUTION_STAGE;
            // Note: Full implementation would check level/grade requirements and materials
            return angel.getIsActive() && hasEvolution;
        } catch (AngelServiceException e) {
            return false;
        }
    }
    
    @Override
    @Transactional
    public Angel renameAngel(Long userId, Integer angelIndex, String newName) {
        log.info("Renaming angel for user: {}, index: {}, newName: {}", userId, angelIndex, newName);

        if (newName == null || newName.trim().isEmpty()) {
            throw new AngelServiceException("Name cannot be empty", "INVALID_NAME");
        }
        if (newName.trim().length() > MAX_NAME_LENGTH) {
            throw new AngelServiceException(
                "Name too long (max " + MAX_NAME_LENGTH + " chars)", "NAME_TOO_LONG");
        }

        Angel angel = getAngel(userId, angelIndex);
        consumeGold(userId.toString(), RENAME_GOLD_COST, "angel_rename");

        angel.setName(newName.trim());
        Angel saved = angelRepository.save(angel);
        log.info("Angel renamed: userId={}, angelIndex={}, name={}", userId, angelIndex, saved.getName());
        return saved;
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
            .reason(2002) // Angel system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new AngelServiceException("Insufficient gold or wallet error", "WALLET_ERROR");
        }
    }
    
    /**
     * Consume material from player bag via bag-service
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
            throw new AngelServiceException("Insufficient materials or bag error", "BAG_ERROR");
        }
    }
}
