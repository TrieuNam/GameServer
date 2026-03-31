package com.SouthMillion.territory_service.service;

import com.SouthMillion.territory_service.model.entity.Territory;
import com.SouthMillion.territory_service.model.entity.TerritoryBuilding;

import java.util.List;

public interface TerritoryService {
    
    // Territory operations
    Territory getTerritory(Long userId);
    
    Territory createTerritory(Long userId, Integer territoryId);
    
    Territory levelUpTerritory(Long userId);
    
    Territory renameTerritory(Long userId, String name);
    
    Territory changeAppearance(Long userId, Integer appearanceId);
    
    // Production operations
    Territory collectResources(Long userId);
    
    void updateProduction(Long userId);
    
    // Building operations
    List<TerritoryBuilding> getAllBuildings(Long userId);
    
    TerritoryBuilding getBuilding(Long userId, Integer slotId);
    
    TerritoryBuilding constructBuilding(Long userId, Integer slotId, Integer buildingId);
    
    TerritoryBuilding upgradeBuilding(Long userId, Integer slotId);
    
    TerritoryBuilding finishConstruction(Long userId, Integer slotId);
    
    TerritoryBuilding instantFinish(Long userId, Integer slotId);
    
    void demolishBuilding(Long userId, Integer slotId);
    
    // Query operations
    List<TerritoryBuilding> getCompletedConstructions(Long userId);
    
    Long getTotalDefense(Long userId);
    
    Long getTotalAttack(Long userId);
    
    Long getTotalProsperity(Long userId);
    
    // Validation
    boolean canLevelUp(Long userId);
    
    boolean canConstruct(Long userId, Integer slotId);
    
    boolean canUpgradeBuilding(Long userId, Integer slotId);
}
