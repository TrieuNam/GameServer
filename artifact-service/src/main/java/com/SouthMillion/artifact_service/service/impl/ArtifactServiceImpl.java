package com.SouthMillion.artifact_service.service.impl;

import com.SouthMillion.artifact_service.client.BagClient;
import com.SouthMillion.artifact_service.client.RoleClient;
import com.SouthMillion.artifact_service.client.WalletClient;
import com.SouthMillion.artifact_service.exception.ArtifactServiceException;
import com.SouthMillion.artifact_service.model.entity.Artifact;
import com.SouthMillion.artifact_service.model.entity.ArtifactDrawRecord;
import com.SouthMillion.artifact_service.repository.ArtifactRepository;
import com.SouthMillion.artifact_service.repository.ArtifactDrawRecordRepository;
import com.SouthMillion.artifact_service.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactServiceImpl implements ArtifactService {
    
    private final ArtifactRepository artifactRepository;
    private final ArtifactDrawRecordRepository drawRecordRepository;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    private final RoleClient roleClient;
    private final Random random = new Random();
    
    private static final int MAX_LEVEL = 100;
    private static final int MAX_GRADE = 10;
    private static final int MAX_REFINEMENT = 15;
    private static final int MAX_AWAKENING = 7;
    private static final int MAX_BLESSING = 10;
    
    @Override
    public List<Artifact> getAllArtifacts(Long userId) {
        log.debug("Getting all artifacts for user: {}", userId);
        return artifactRepository.findByUserId(userId);
    }
    
    @Override
    public Artifact getArtifact(Long userId, Integer artifactIndex) {
        log.debug("Getting artifact for user: {}, index: {}", userId, artifactIndex);
        return artifactRepository.findByUserIdAndArtifactIndex(userId, artifactIndex)
            .orElseThrow(() -> new ArtifactServiceException(
                "Artifact not found: userId=" + userId + ", index=" + artifactIndex,
                "ARTIFACT_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Artifact unlockArtifact(Long userId, Integer artifactId) {
        log.info("Unlocking artifact for user: {}, artifactId: {}", userId, artifactId);
        
        if (artifactRepository.findByUserIdAndArtifactId(userId, artifactId).isPresent()) {
            throw new ArtifactServiceException("Artifact already unlocked", "ARTIFACT_ALREADY_EXISTS");
        }
        
        List<Artifact> existing = artifactRepository.findByUserId(userId);
        int nextIndex = existing.size();
        
        Artifact artifact = new Artifact();
        artifact.setUserId(userId);
        artifact.setArtifactIndex(nextIndex);
        artifact.setArtifactId(artifactId);
        artifact.setLevel(1);
        artifact.setGrade(1);
        artifact.setExp(0L);
        artifact.setIsActive(true);
        artifact.setIsEquipped(false);
        artifact.setRefinementLevel(0);
        artifact.setAwakeningStage(0);
        artifact.setSoulPower(0L);
        artifact.setDivineEssence(0L);
        artifact.setBlessingTier(0);
        
        generateRandomAttributes(artifact);
        
        Artifact saved = artifactRepository.save(artifact);
        log.info("Artifact unlocked: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public Artifact levelUpArtifact(Long userId, Integer artifactIndex) {
        log.info("Leveling up artifact for user: {}, index: {}", userId, artifactIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if (!artifact.getIsActive()) {
            throw new ArtifactServiceException("Artifact not activated", "ARTIFACT_NOT_ACTIVE");
        }
        
        if (artifact.getLevel() >= MAX_LEVEL) {
            throw new ArtifactServiceException("Artifact at max level", "MAX_LEVEL_REACHED");
        }
        
        artifact.setLevel(artifact.getLevel() + 1);
        artifact.setExp(0L);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public Artifact gradeUpArtifact(Long userId, Integer artifactIndex) {
        log.info("Upgrading artifact grade for user: {}, index: {}", userId, artifactIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if (artifact.getGrade() >= MAX_GRADE) {
            throw new ArtifactServiceException("Artifact at max grade", "MAX_GRADE_REACHED");
        }
        
        artifact.setGrade(artifact.getGrade() + 1);
        artifact.setLevel(1);
        artifact.setExp(0L);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public void equipArtifact(Long userId, Integer artifactIndex) {
        log.info("Equipping artifact for user: {}, index: {}", userId, artifactIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if (!artifact.getIsActive()) {
            throw new ArtifactServiceException("Cannot equip inactive artifact", "ARTIFACT_NOT_ACTIVE");
        }
        
        artifactRepository.unequipAllArtifacts(userId);
        artifact.setIsEquipped(true);
        artifactRepository.save(artifact);
        
        log.info("Artifact equipped: {}", artifactIndex);
    }
    
    @Override
    @Transactional
    public void unequipArtifact(Long userId) {
        log.info("Unequipping artifact for user: {}", userId);
        artifactRepository.unequipAllArtifacts(userId);
    }
    
    @Override
    @Transactional
    public Artifact refineArtifact(Long userId, Integer artifactIndex) {
        log.info("Refining artifact for user: {}, index: {}", userId, artifactIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if (artifact.getRefinementLevel() >= MAX_REFINEMENT) {
            throw new ArtifactServiceException("Artifact at max refinement", "MAX_REFINEMENT_REACHED");
        }
        
        artifact.setRefinementLevel(artifact.getRefinementLevel() + 1);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public Artifact awakenArtifact(Long userId, Integer artifactIndex) {
        log.info("Awakening artifact for user: {}, index: {}", userId, artifactIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if (artifact.getAwakeningStage() >= MAX_AWAKENING) {
            throw new ArtifactServiceException("Artifact at max awakening", "MAX_AWAKENING_REACHED");
        }
        
        artifact.setAwakeningStage(artifact.getAwakeningStage() + 1);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public Artifact addSoulPower(Long userId, Integer artifactIndex, Long points) {
        log.info("Adding soul power to artifact for user: {}, index: {}, points: {}", 
            userId, artifactIndex, points);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        artifact.setSoulPower(artifact.getSoulPower() + points);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public Artifact addDivineEssence(Long userId, Integer artifactIndex, Long amount) {
        log.info("Adding divine essence to artifact for user: {}, index: {}, amount: {}", 
            userId, artifactIndex, amount);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        artifact.setDivineEssence(artifact.getDivineEssence() + amount);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public Artifact upgradeBlessing(Long userId, Integer artifactIndex) {
        log.info("Upgrading blessing for artifact user: {}, index: {}", userId, artifactIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if (artifact.getBlessingTier() >= MAX_BLESSING) {
            throw new ArtifactServiceException("Blessing at max tier", "MAX_BLESSING_REACHED");
        }
        
        artifact.setBlessingTier(artifact.getBlessingTier() + 1);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public Artifact refreshAttributes(Long userId, Integer artifactIndex, Integer lockFlag) {
        log.info("Refreshing artifact attributes for user: {}, index: {}, lockFlag: {}", 
            userId, artifactIndex, lockFlag);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        if ((lockFlag & 1) == 0) {
            int[] attr = generateRandomAttribute(artifact.getGrade());
            artifact.setAttr1Type(attr[0]);
            artifact.setAttr1Value((long) attr[1]);
        }
        
        if ((lockFlag & 2) == 0) {
            int[] attr = generateRandomAttribute(artifact.getGrade());
            artifact.setAttr2Type(attr[0]);
            artifact.setAttr2Value((long) attr[1]);
        }
        
        if ((lockFlag & 4) == 0) {
            int[] attr = generateRandomAttribute(artifact.getGrade());
            artifact.setAttr3Type(attr[0]);
            artifact.setAttr3Value((long) attr[1]);
        }
        
        if ((lockFlag & 8) == 0) {
            int[] attr = generateRandomAttribute(artifact.getGrade());
            artifact.setAttr4Type(attr[0]);
            artifact.setAttr4Value((long) attr[1]);
        }
        
        return artifactRepository.save(artifact);
    }
    
    private void generateRandomAttributes(Artifact artifact) {
        for (int i = 0; i < 4; i++) {
            int[] attr = generateRandomAttribute(artifact.getGrade());
            switch (i) {
                case 0 -> {
                    artifact.setAttr1Type(attr[0]);
                    artifact.setAttr1Value((long) attr[1]);
                }
                case 1 -> {
                    artifact.setAttr2Type(attr[0]);
                    artifact.setAttr2Value((long) attr[1]);
                }
                case 2 -> {
                    artifact.setAttr3Type(attr[0]);
                    artifact.setAttr3Value((long) attr[1]);
                }
                case 3 -> {
                    artifact.setAttr4Type(attr[0]);
                    artifact.setAttr4Value((long) attr[1]);
                }
            }
        }
    }
    
    private int[] generateRandomAttribute(Integer grade) {
        int type = random.nextInt(10) + 1;
        int baseValue = 200 * grade;
        int value = baseValue + random.nextInt(baseValue / 2);
        return new int[]{type, value};
    }
    
    @Override
    public Long calculateArtifactPower(Artifact artifact) {
        if (!artifact.getIsActive()) {
            return 0L;
        }
        
        long basePower = artifact.getLevel() * 200L;
        long gradePower = artifact.getGrade() * 1000L;
        long refinementPower = artifact.getRefinementLevel() * 500L;
        long awakeningPower = artifact.getAwakeningStage() * 2000L;
        long soulPower = artifact.getSoulPower() / 5;
        long divinePower = artifact.getDivineEssence() / 10;
        long blessingPower = artifact.getBlessingTier() * 800L;
        
        long attrPower = 0L;
        if (artifact.getAttr1Value() != null) attrPower += artifact.getAttr1Value();
        if (artifact.getAttr2Value() != null) attrPower += artifact.getAttr2Value();
        if (artifact.getAttr3Value() != null) attrPower += artifact.getAttr3Value();
        if (artifact.getAttr4Value() != null) attrPower += artifact.getAttr4Value();
        
        return basePower + gradePower + refinementPower + awakeningPower + 
               soulPower + divinePower + blessingPower + attrPower;
    }
    
    @Override
    public boolean canLevelUp(Long userId, Integer artifactIndex) {
        try {
            Artifact artifact = getArtifact(userId, artifactIndex);
            return artifact.getIsActive() && artifact.getLevel() < MAX_LEVEL;
        } catch (ArtifactServiceException e) {
            return false;
        }
    }
    
    @Override
    public boolean canGradeUp(Long userId, Integer artifactIndex) {
        try {
            Artifact artifact = getArtifact(userId, artifactIndex);
            return artifact.getIsActive() && artifact.getGrade() < MAX_GRADE;
        } catch (ArtifactServiceException e) {
            return false;
        }
    }
    
    @Override
    public boolean canRefine(Long userId, Integer artifactIndex) {
        try {
            Artifact artifact = getArtifact(userId, artifactIndex);
            return artifact.getIsActive() && artifact.getRefinementLevel() < MAX_REFINEMENT;
        } catch (ArtifactServiceException e) {
            return false;
        }
    }
    
    @Override
    public boolean canAwaken(Long userId, Integer artifactIndex) {
        try {
            Artifact artifact = getArtifact(userId, artifactIndex);
            return artifact.getIsActive() && artifact.getAwakeningStage() < MAX_AWAKENING;
        } catch (ArtifactServiceException e) {
            return false;
        }
    }
    
    @Override
    @Transactional
    public Artifact upgradeArtifactSkill(Long userId, Integer artifactIndex, Integer skillIndex) {
        log.info("Upgrading artifact skill - userId: {}, artifactIndex: {}, skillIndex: {}", 
                userId, artifactIndex, skillIndex);
        
        Artifact artifact = getArtifact(userId, artifactIndex);
        
        // Validate artifact is active
        if (!artifact.getIsActive()) {
            throw new ArtifactServiceException("Artifact not unlocked", "NOT_UNLOCKED");
        }
        
        // Validate skill index (0-2 for 3 skills)
        if (skillIndex < 0 || skillIndex > 2) {
            throw new ArtifactServiceException("Invalid skill index: " + skillIndex, "INVALID_SKILL");
        }
        
        // Get current skill level
        int currentLevel;
        switch (skillIndex) {
            case 0 -> currentLevel = artifact.getSkill1Level();
            case 1 -> currentLevel = artifact.getSkill2Level();
            case 2 -> currentLevel = artifact.getSkill3Level();
            default -> throw new ArtifactServiceException("Invalid skill index", "INVALID_SKILL");
        }
        
        // Max skill level is 10 or artifact level (whichever is lower)
        int maxSkillLevel = Math.min(10, artifact.getLevel());
        if (currentLevel >= maxSkillLevel) {
            throw new ArtifactServiceException(
                "Skill already at max level: " + maxSkillLevel, "MAX_LEVEL");
        }
        
        // Calculate cost (increases with level)
        int skillUpgradeCost = 1000 * (currentLevel + 1);
        
        // Consume currency (gold)
        consumeCurrency(String.valueOf(userId), skillUpgradeCost, 1, "Artifact skill upgrade");
        
        // Upgrade skill
        int newLevel = currentLevel + 1;
        switch (skillIndex) {
            case 0 -> artifact.setSkill1Level(newLevel);
            case 1 -> artifact.setSkill2Level(newLevel);
            case 2 -> artifact.setSkill3Level(newLevel);
        }
        
        log.info("Upgraded artifact skill - userId: {}, artifactIndex: {}, skillIndex: {}, {} -> {}", 
                userId, artifactIndex, skillIndex, currentLevel, newLevel);
        
        return artifactRepository.save(artifact);
    }
    
    @Override
    @Transactional
    public List<Map<String, Object>> drawArtifacts(Long userId, Integer drawType) {
        log.info("Drawing artifacts - userId: {}, drawType: {}", userId, drawType);
        
        // Validate draw type (1=single, 10=10-pull)
        if (drawType != 1 && drawType != 10) {
            throw new ArtifactServiceException("Invalid draw type: " + drawType, "INVALID_DRAW_TYPE");
        }
        
        int numDraws = drawType;
        
        // Calculate cost (100 diamonds per draw, 900 for 10-pull)
        long cost = (drawType == 10) ? 900 : 100;
        consumeCurrency(String.valueOf(userId), cost, 2, "Artifact gacha"); // 2=diamond
        
        // Perform draws
        List<Map<String, Object>> results = new ArrayList<>();
        long totalDraws = drawRecordRepository.countByUserId(userId);
        
        for (int i = 0; i < numDraws; i++) {
            totalDraws++;
            
            // Gacha logic: pity system at 80 draws guarantees legendary (quality 4)
            boolean isGuaranteed = (totalDraws % 80 == 0);
            int quality;
            int artifactId;
            
            if (isGuaranteed) {
                quality = 4; // Legendary
                artifactId = 10 + random.nextInt(5); // Legendary artifacts 10-14
            } else {
                // Normal rates: 60% common, 25% rare, 12% epic, 3% legendary
                int roll = random.nextInt(100);
                if (roll < 60) {
                    quality = 1; // Common
                    artifactId = 1 + random.nextInt(4); // 1-4
                } else if (roll < 85) {
                    quality = 2; // Rare
                    artifactId = 5 + random.nextInt(3); // 5-7
                } else if (roll < 97) {
                    quality = 3; // Epic
                    artifactId = 8 + random.nextInt(2); // 8-9
                } else {
                    quality = 4; // Legendary
                    artifactId = 10 + random.nextInt(5); // 10-14
                }
            }
            
            // Save draw record
            ArtifactDrawRecord record = ArtifactDrawRecord.builder()
                    .userId(userId)
                    .drawType(drawType)
                    .artifactId(artifactId)
                    .quality(quality)
                    .isGuaranteed(isGuaranteed)
                    .costType(2) // Diamond
                    .costAmount(cost / numDraws)
                    .build();
            drawRecordRepository.save(record);
            
            // Add to results
            Map<String, Object> result = new HashMap<>();
            result.put("artifactId", artifactId);
            result.put("quality", quality);
            result.put("isGuaranteed", isGuaranteed);
            results.add(result);
            
            log.debug("Drew artifact: id={}, quality={}, guaranteed={}", artifactId, quality, isGuaranteed);
        }
        
        log.info("Completed {} draws for userId: {}", numDraws, userId);
        return results;
    }
    
    @Override
    public List<ArtifactDrawRecord> getDrawRecords(Long userId) {
        log.debug("Getting draw records for userId: {}", userId);
        return drawRecordRepository.findTop100ByUserIdOrderByDrawTimestampDesc(userId);
    }
    
    /**
     * Consume gold/currency from player wallet
     */
    private void consumeCurrency(String playerId, long amount, int currencyId, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId((long) currencyId)
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(4002) // Artifact system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} of currency {} for {}", amount, currencyId, reason);
        } catch (Exception e) {
            log.error("Failed to consume currency: {}", e.getMessage());
            throw new ArtifactServiceException("Insufficient currency: " + e.getMessage(), "WALLET_ERROR");
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
            throw new ArtifactServiceException("Insufficient materials: " + e.getMessage(), "BAG_ERROR");
        }
    }
}
