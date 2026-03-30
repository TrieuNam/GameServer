package com.SouthMillion.territory_service.service.impl;

import com.SouthMillion.territory_service.client.BagClient;
import com.SouthMillion.territory_service.client.WalletClient;
import com.SouthMillion.territory_service.exception.TerritoryServiceException;
import com.SouthMillion.territory_service.model.entity.Territory;
import com.SouthMillion.territory_service.model.entity.TerritoryBuilding;
import com.SouthMillion.territory_service.repository.TerritoryBuildingRepository;
import com.SouthMillion.territory_service.repository.TerritoryRepository;
import com.SouthMillion.territory_service.service.TerritoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerritoryServiceImpl implements TerritoryService {
    
    private final TerritoryRepository territoryRepository;
    private final TerritoryBuildingRepository buildingRepository;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    
    private static final int MAX_TERRITORY_LEVEL = 50;
    private static final int BUILDING_STATUS_EMPTY = 0;
    private static final int BUILDING_STATUS_CONSTRUCTING = 1;
    private static final int BUILDING_STATUS_BUILT = 2;
    private static final int BUILDING_STATUS_UPGRADING = 3;
    
    @Override
    public Territory getTerritory(Long userId) {
        return territoryRepository.findByUserId(userId)
            .orElseThrow(() -> new TerritoryServiceException(
                "Territory not found for user: " + userId,
                "TERRITORY_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Territory createTerritory(Long userId, Integer territoryId) {
        log.info("Creating territory for user: {}, territoryId: {}", userId, territoryId);
        
        if (territoryRepository.existsByUserId(userId)) {
            throw new TerritoryServiceException("Territory already exists", "TERRITORY_EXISTS");
        }
        
        Territory territory = new Territory();
        territory.setUserId(userId);
        territory.setTerritoryId(territoryId);
        territory.setLevel(1);
        territory.setName("My Territory");
        territory.setProsperity(0L);
        territory.setDefenseRating(100L);
        territory.setAttackRating(100L);
        territory.setGoldProduction(100L);
        territory.setResourceProduction(50L);
        territory.setAccumulatedGold(0L);
        territory.setAccumulatedResources(0L);
        territory.setLastProductionTime(LocalDateTime.now());
        territory.setMaxGoldStorage(10000L);
        territory.setMaxResourceStorage(5000L);
        territory.setBuildingSlots(10);
        territory.setAppearanceId(1);
        
        Territory saved = territoryRepository.save(territory);
        log.info("Territory created: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public Territory levelUpTerritory(Long userId) {
        log.info("Leveling up territory for user: {}", userId);
        
        Territory territory = getTerritory(userId);
        
        if (territory.getLevel() >= MAX_TERRITORY_LEVEL) {
            throw new TerritoryServiceException("Territory at max level", "MAX_TERRITORY_LEVEL");
        }
        
        // Calculate costs based on current level
        int level = territory.getLevel();
        long goldCost = 1000L * level; // Base cost increases with level
        int materialId = 10001; // Territory upgrade material
        int materialQuantity = level * 2;
        
        // Consume materials
        consumeGold(userId.toString(), goldCost, "territory_levelup");
        consumeMaterial(userId.toString(), materialId, materialQuantity);
        
        territory.setLevel(territory.getLevel() + 1);
        territory.setBuildingSlots(territory.getBuildingSlots() + 1);
        territory.setMaxGoldStorage(territory.getMaxGoldStorage() + 1000L);
        territory.setMaxResourceStorage(territory.getMaxResourceStorage() + 500L);
        
        return territoryRepository.save(territory);
    }
    
    @Override
    @Transactional
    public Territory renameTerritory(Long userId, String name) {
        log.info("Renaming territory for user: {}, name: {}", userId, name);
        
        Territory territory = getTerritory(userId);
        
        if (name == null || name.trim().isEmpty() || name.length() > 50) {
            throw new TerritoryServiceException("Invalid territory name", "INVALID_NAME");
        }
        
        territory.setName(name.trim());
        
        return territoryRepository.save(territory);
    }
    
    @Override
    @Transactional
    public Territory changeAppearance(Long userId, Integer appearanceId) {
        log.info("Changing territory appearance for user: {}, appearanceId: {}", userId, appearanceId);
        
        Territory territory = getTerritory(userId);
        
        // Validate appearance ID (basic validation, real implementation would check config)
        if (appearanceId == null || appearanceId < 1) {
            throw new TerritoryServiceException("Invalid appearance ID", "INVALID_APPEARANCE");
        }
        
        territory.setAppearanceId(appearanceId);
        
        return territoryRepository.save(territory);
    }
    
    @Override
    @Transactional
    public Territory collectResources(Long userId) {
        log.info("Collecting resources for user: {}", userId);
        
        Territory territory = getTerritory(userId);
        
        // Calculate accumulated resources
        updateProduction(userId);
        territory = getTerritory(userId);
        
        long collectedGold = territory.getAccumulatedGold();
        long collectedResources = territory.getAccumulatedResources();
        
        if (collectedGold == 0 && collectedResources == 0) {
            throw new TerritoryServiceException("No resources to collect", "NO_RESOURCES");
        }
        
        // Add resources to wallet
        addGold(userId.toString(), collectedGold, "territory_production");
        if (collectedResources > 0) {
            addResource(userId.toString(), collectedResources, "territory_production");
        }
        
        territory.setAccumulatedGold(0L);
        territory.setAccumulatedResources(0L);
        territory.setLastProductionTime(LocalDateTime.now());
        
        log.info("Collected gold: {}, resources: {}", collectedGold, collectedResources);
        
        return territoryRepository.save(territory);
    }
    
    @Override
    @Transactional
    public void updateProduction(Long userId) {
        Territory territory = getTerritory(userId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastProduction = territory.getLastProductionTime();
        
        if (lastProduction == null) {
            lastProduction = now;
            territory.setLastProductionTime(lastProduction);
        }
        
        Duration duration = Duration.between(lastProduction, now);
        long hours = duration.toHours();
        
        if (hours > 0) {
            long goldProduced = territory.getGoldProduction() * hours;
            long resourcesProduced = territory.getResourceProduction() * hours;
            
            // Apply storage caps
            long newAccumulatedGold = Math.min(
                territory.getAccumulatedGold() + goldProduced,
                territory.getMaxGoldStorage()
            );
            long newAccumulatedResources = Math.min(
                territory.getAccumulatedResources() + resourcesProduced,
                territory.getMaxResourceStorage()
            );
            
            territory.setAccumulatedGold(newAccumulatedGold);
            territory.setAccumulatedResources(newAccumulatedResources);
            territory.setLastProductionTime(now);
            
            territoryRepository.save(territory);
        }
    }
    
    @Override
    public List<TerritoryBuilding> getAllBuildings(Long userId) {
        return buildingRepository.findByUserId(userId);
    }
    
    @Override
    public TerritoryBuilding getBuilding(Long userId, Integer slotId) {
        return buildingRepository.findByUserIdAndSlotId(userId, slotId)
            .orElseThrow(() -> new TerritoryServiceException(
                "Building not found: userId=" + userId + ", slotId=" + slotId,
                "BUILDING_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public TerritoryBuilding constructBuilding(Long userId, Integer slotId, Integer buildingId) {
        log.info("Constructing building for user: {}, slot: {}, buildingId: {}", 
            userId, slotId, buildingId);
        
        Territory territory = getTerritory(userId);
        
        if (slotId < 1 || slotId > territory.getBuildingSlots()) {
            throw new TerritoryServiceException("Invalid building slot", "INVALID_SLOT");
        }
        
        if (buildingRepository.existsByUserIdAndSlotId(userId, slotId)) {
            throw new TerritoryServiceException("Slot already occupied", "SLOT_OCCUPIED");
        }
        
        // Calculate construction costs
        long goldCost = 5000L; // Base construction cost
        int woodId = 10002; // Wood material
        int stoneId = 10003; // Stone material
        int woodQuantity = 50;
        int stoneQuantity = 30;
        
        // Consume construction materials
        consumeGold(userId.toString(), goldCost, "building_construct");
        consumeMaterial(userId.toString(), woodId, woodQuantity);
        consumeMaterial(userId.toString(), stoneId, stoneQuantity);
        
        TerritoryBuilding building = new TerritoryBuilding();
        building.setUserId(userId);
        building.setSlotId(slotId);
        building.setBuildingId(buildingId);
        building.setLevel(1);
        building.setStatus(BUILDING_STATUS_CONSTRUCTING);
        building.setStartTime(LocalDateTime.now());
        // Building construction takes 2 hours (configurable via application.yml)
        building.setFinishTime(LocalDateTime.now().plusHours(2));
        building.setProductionRate(0L);
        building.setDefenseContribution(100L);
        building.setAttackContribution(50L);
        building.setProsperityContribution(10L);
        
        TerritoryBuilding saved = buildingRepository.save(building);
        log.info("Building construction started: {}", saved);
        return saved;
    }
    
    @Override
    @Transactional
    public TerritoryBuilding upgradeBuilding(Long userId, Integer slotId) {
        log.info("Upgrading building for user: {}, slot: {}", userId, slotId);
        
        TerritoryBuilding building = getBuilding(userId, slotId);
        
        if (building.getStatus() != BUILDING_STATUS_BUILT) {
            throw new TerritoryServiceException("Building not ready for upgrade", "BUILDING_NOT_BUILT");
        }
        
        // Calculate upgrade costs based on level
        int level = building.getLevel();
        long goldCost = 2000L * level;
        int materialId = 10002; // Wood material
        int materialQuantity = 20 * level;
        
        // Consume upgrade materials
        consumeGold(userId.toString(), goldCost, "building_upgrade");
        consumeMaterial(userId.toString(), materialId, materialQuantity);
        
        building.setStatus(BUILDING_STATUS_UPGRADING);
        building.setStartTime(LocalDateTime.now());
        // Building upgrade takes 1 hour (configurable via application.yml)
        building.setFinishTime(LocalDateTime.now().plusHours(1));
        
        return buildingRepository.save(building);
    }
    
    @Override
    @Transactional
    public TerritoryBuilding finishConstruction(Long userId, Integer slotId) {
        log.info("Finishing construction for user: {}, slot: {}", userId, slotId);
        
        TerritoryBuilding building = getBuilding(userId, slotId);
        
        if (building.getStatus() != BUILDING_STATUS_CONSTRUCTING && 
            building.getStatus() != BUILDING_STATUS_UPGRADING) {
            throw new TerritoryServiceException("No construction in progress", "NO_CONSTRUCTION");
        }
        
        if (building.getFinishTime() == null || building.getFinishTime().isAfter(LocalDateTime.now())) {
            throw new TerritoryServiceException("Construction not finished yet", "CONSTRUCTION_NOT_FINISHED");
        }
        
        if (building.getStatus() == BUILDING_STATUS_UPGRADING) {
            building.setLevel(building.getLevel() + 1);
        }
        
        building.setStatus(BUILDING_STATUS_BUILT);
        building.setStartTime(null);
        building.setFinishTime(null);
        
        // Update contributions based on level
        building.setDefenseContribution(100L * building.getLevel());
        building.setAttackContribution(50L * building.getLevel());
        building.setProsperityContribution(10L * building.getLevel());
        building.setProductionRate(20L * building.getLevel());
        
        TerritoryBuilding saved = buildingRepository.save(building);
        
        // Update territory stats
        updateTerritoryStats(userId);
        
        return saved;
    }
    
    @Override
    @Transactional
    public TerritoryBuilding instantFinish(Long userId, Integer slotId) {
        log.info("Instant finishing construction for user: {}, slot: {}", userId, slotId);
        
        TerritoryBuilding building = getBuilding(userId, slotId);
        
        if (building.getStatus() != BUILDING_STATUS_CONSTRUCTING && 
            building.getStatus() != BUILDING_STATUS_UPGRADING) {
            throw new TerritoryServiceException("No construction in progress", "NO_CONSTRUCTION");
        }
        
        // Consume instant finish item or gems
        int instantFinishItemId = 10010; // Speed-up item
        int gemCost = 100; // Alternative: use gems
        
        try {
            // Try using instant finish item first
            consumeMaterial(userId.toString(), instantFinishItemId, 1);
        } catch (Exception e) {
            // Fall back to gems if no item available
            consumeGold(userId.toString(), gemCost, "building_instant_finish");
        }
        
        if (building.getStatus() == BUILDING_STATUS_UPGRADING) {
            building.setLevel(building.getLevel() + 1);
        }
        
        building.setStatus(BUILDING_STATUS_BUILT);
        building.setStartTime(null);
        building.setFinishTime(null);
        
        // Update contributions
        building.setDefenseContribution(100L * building.getLevel());
        building.setAttackContribution(50L * building.getLevel());
        building.setProsperityContribution(10L * building.getLevel());
        building.setProductionRate(20L * building.getLevel());
        
        TerritoryBuilding saved = buildingRepository.save(building);
        
        // Update territory stats
        updateTerritoryStats(userId);
        
        return saved;
    }
    
    @Override
    @Transactional
    public void demolishBuilding(Long userId, Integer slotId) {
        log.info("Demolishing building for user: {}, slot: {}", userId, slotId);
        
        TerritoryBuilding building = getBuilding(userId, slotId);
        
        // Refund some materials (50% of construction cost)
        long goldRefund = 2500L;
        addGold(userId.toString(), goldRefund, "building_demolish_refund");
        
        buildingRepository.delete(building);
        
        // Update territory stats
        updateTerritoryStats(userId);
    }
    
    @Override
    public List<TerritoryBuilding> getCompletedConstructions(Long userId) {
        return buildingRepository.findCompletedConstructions(userId, LocalDateTime.now());
    }
    
    @Override
    public Long getTotalDefense(Long userId) {
        Territory territory = getTerritory(userId);
        Long buildingDefense = buildingRepository.getTotalDefense(userId);
        return territory.getDefenseRating() + (buildingDefense != null ? buildingDefense : 0L);
    }
    
    @Override
    public Long getTotalAttack(Long userId) {
        Territory territory = getTerritory(userId);
        Long buildingAttack = buildingRepository.getTotalAttack(userId);
        return territory.getAttackRating() + (buildingAttack != null ? buildingAttack : 0L);
    }
    
    @Override
    public Long getTotalProsperity(Long userId) {
        Territory territory = getTerritory(userId);
        Long buildingProsperity = buildingRepository.getTotalProsperity(userId);
        return territory.getProsperity() + (buildingProsperity != null ? buildingProsperity : 0L);
    }
    
    @Override
    public boolean canLevelUp(Long userId) {
        try {
            Territory territory = getTerritory(userId);
            if (territory.getLevel() >= MAX_TERRITORY_LEVEL) {
                return false;
            }
            // Material check would query wallet/bag service for sufficient resources
            // For now, return true if level limit not reached
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean canConstruct(Long userId, Integer slotId) {
        try {
            Territory territory = getTerritory(userId);
            if (slotId < 1 || slotId > territory.getBuildingSlots()) {
                return false;
            }
            if (buildingRepository.existsByUserIdAndSlotId(userId, slotId)) {
                return false;
            }
            // Material check would query wallet/bag service for sufficient resources
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean canUpgradeBuilding(Long userId, Integer slotId) {
        try {
            TerritoryBuilding building = getBuilding(userId, slotId);
            if (building.getStatus() != BUILDING_STATUS_BUILT) {
                return false;
            }
            // Material check would query wallet/bag service for sufficient resources
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void updateTerritoryStats(Long userId) {
        Territory territory = getTerritory(userId);
        
        Long totalDefense = buildingRepository.getTotalDefense(userId);
        Long totalAttack = buildingRepository.getTotalAttack(userId);
        Long totalProsperity = buildingRepository.getTotalProsperity(userId);
        Long totalProduction = buildingRepository.getTotalProductionRate(userId);
        
        territory.setDefenseRating(100L + (totalDefense != null ? totalDefense : 0L));
        territory.setAttackRating(100L + (totalAttack != null ? totalAttack : 0L));
        territory.setProsperity(totalProsperity != null ? totalProsperity : 0L);
        territory.setGoldProduction(100L + (totalProduction != null ? totalProduction : 0L));
        territory.setResourceProduction(50L + (totalProduction != null ? totalProduction / 2 : 0L));
        
        territoryRepository.save(territory);
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
            .reason(3001) // Territory system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume gold: {}", e.getMessage());
            throw new TerritoryServiceException("Insufficient gold or wallet error", "WALLET_ERROR");
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
            .reason(3001) // Territory system reason code
            .build();
        
        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} gold for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to add gold: {}", e.getMessage());
            throw new TerritoryServiceException("Wallet error", "WALLET_ERROR");
        }
    }
    
    /**
     * Add resource to player wallet via wallet-service
     */
    private void addResource(String playerId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(2L) // Resource currency
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(playerId)
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(3001) // Territory system reason code
            .build();
        
        try {
            walletClient.addCurrency(playerId, request);
            log.debug("Added {} resources for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to add resources: {}", e.getMessage());
            throw new TerritoryServiceException("Wallet error", "WALLET_ERROR");
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
            throw new TerritoryServiceException("Insufficient materials or bag error", "BAG_ERROR");
        }
    }
}
