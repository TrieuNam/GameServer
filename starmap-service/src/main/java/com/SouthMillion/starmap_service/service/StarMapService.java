package com.SouthMillion.starmap_service.service;

import com.SouthMillion.starmap_service.model.entity.Constellation;
import com.SouthMillion.starmap_service.model.entity.Star;

import java.util.List;

public interface StarMapService {
    
    // Star operations
    List<Star> getAllStars(Long userId);
    
    Star getStar(Long userId, Integer starId);
    
    Star activateStar(Long userId, Integer starId);
    
    Star levelUpStar(Long userId, Integer starId);
    
    Star addStarEnergy(Long userId, Integer starId, Long energy);
    
    // Constellation operations
    List<Constellation> getAllConstellations(Long userId);
    
    Constellation getConstellation(Long userId, Integer constellationId);
    
    Constellation unlockConstellation(Long userId, Integer constellationId);
    
    Constellation levelUpConstellation(Long userId, Integer constellationId);
    
    void checkConstellationCompletion(Long userId, Integer constellationId);
    
    // Power calculations
    Long calculateTotalStarMapPower(Long userId);
    
    Long calculateConstellationPower(Constellation constellation);
}
