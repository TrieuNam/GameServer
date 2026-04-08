package com.SouthMillion.starmap_service.service.impl;

import com.SouthMillion.starmap_service.client.BagClient;
import com.SouthMillion.starmap_service.client.RoleClient;
import com.SouthMillion.starmap_service.client.RoleServiceClient;
import com.SouthMillion.starmap_service.client.WalletClient;
import com.SouthMillion.starmap_service.config.StarmapConfigProvider;
import com.SouthMillion.starmap_service.exception.StarMapServiceException;
import com.SouthMillion.starmap_service.model.config.StarmapConfig;
import com.SouthMillion.starmap_service.model.entity.Constellation;
import com.SouthMillion.starmap_service.model.entity.Star;
import com.SouthMillion.starmap_service.repository.ConstellationRepository;
import com.SouthMillion.starmap_service.repository.StarRepository;
import com.SouthMillion.starmap_service.service.StarMapService;
import com.SouthMillion.starmap_service.util.StarmapPowerCalculator;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StarMapServiceImpl implements StarMapService {

    private final StarRepository starRepository;
    private final ConstellationRepository constellationRepository;
    private final BagClient bagClient;
    private final WalletClient walletClient;
    private final RoleClient roleClient;
    private final StarmapPowerCalculator powerCalculator;
    private final RoleServiceClient roleServiceClient;
    private final StarmapConfigProvider configProvider;
    
    private static final int MAX_STAR_LEVEL = 50;
    private static final int MAX_CONSTELLATION_LEVEL = 20;
    
    @Override
    public List<Star> getAllStars(Long userId) {
        return starRepository.findByUserId(userId);
    }
    
    @Override
    public Star getStar(Long userId, Integer starId) {
        return starRepository.findByUserIdAndStarId(userId, starId)
            .orElseThrow(() -> new StarMapServiceException(
                "Star not found: userId=" + userId + ", starId=" + starId,
                "STAR_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Star activateStar(Long userId, Integer starId) {
        log.info("Activating star for user: {}, starId: {}", userId, starId);

        Star star = starRepository.findByUserIdAndStarId(userId, starId)
            .orElseGet(() -> {
                Star newStar = new Star();
                newStar.setUserId(userId);
                newStar.setStarId(starId);
                newStar.setLevel(0);
                newStar.setIsActive(false);
                newStar.setEnergy(0L);
                return newStar;
            });

        if (star.getIsActive()) {
            throw new StarMapServiceException("Star already activated", "STAR_ALREADY_ACTIVE");
        }

        star.setIsActive(true);
        star.setLevel(1);

        Star saved = starRepository.save(star);

        // Calculate star power and sync with role-service
        StarmapConfig config = configProvider.getStarmapConfig();
        StarmapConfig.StarItem starConfig = findStarConfig(starId, config);
        Long starPower = powerCalculator.calculateStarPower(saved, starConfig);

        log.info("Star activated: starId={}, power={}", starId, starPower);

        // Update role capability
        updateRoleCapability(String.valueOf(userId), starPower, "star_activated");

        return saved;
    }
    
    @Override
    @Transactional
    public Star levelUpStar(Long userId, Integer starId) {
        log.info("Leveling up star for user: {}, starId: {}", userId, starId);
        
        Star star = getStar(userId, starId);
        
        if (!star.getIsActive()) {
            throw new StarMapServiceException("Star not activated", "STAR_NOT_ACTIVE");
        }
        
        if (star.getLevel() >= MAX_STAR_LEVEL) {
            throw new StarMapServiceException("Star at max level", "MAX_STAR_LEVEL_REACHED");
        }
        
        star.setLevel(star.getLevel() + 1);
        
        return starRepository.save(star);
    }
    
    @Override
    @Transactional
    public Star addStarEnergy(Long userId, Integer starId, Long energy) {
        log.info("Adding energy to star for user: {}, starId: {}, energy: {}", 
            userId, starId, energy);
        
        Star star = getStar(userId, starId);
        star.setEnergy(star.getEnergy() + energy);
        
        return starRepository.save(star);
    }
    
    @Override
    public List<Constellation> getAllConstellations(Long userId) {
        return constellationRepository.findByUserId(userId);
    }
    
    @Override
    public Constellation getConstellation(Long userId, Integer constellationId) {
        return constellationRepository.findByUserIdAndConstellationId(userId, constellationId)
            .orElseThrow(() -> new StarMapServiceException(
                "Constellation not found: userId=" + userId + ", id=" + constellationId,
                "CONSTELLATION_NOT_FOUND"
            ));
    }
    
    @Override
    @Transactional
    public Constellation unlockConstellation(Long userId, Integer constellationId) {
        log.info("Unlocking constellation for user: {}, id: {}", userId, constellationId);

        Constellation constellation = constellationRepository
            .findByUserIdAndConstellationId(userId, constellationId)
            .orElseGet(() -> {
                Constellation newConst = new Constellation();
                newConst.setUserId(userId);
                newConst.setConstellationId(constellationId);
                newConst.setLevel(1);
                newConst.setIsUnlocked(false);
                newConst.setIsCompleted(false);
                newConst.setStarsActivated(0);
                newConst.setTotalStars(10); // Default total stars per constellation
                newConst.setPower(0L);
                return newConst;
            });

        if (constellation.getIsUnlocked()) {
            throw new StarMapServiceException("Constellation already unlocked", "CONSTELLATION_ALREADY_UNLOCKED");
        }

        constellation.setIsUnlocked(true);

        Constellation saved = constellationRepository.save(constellation);

        // Calculate constellation power and sync with role-service
        StarmapConfig config = configProvider.getStarmapConfig();
        StarmapConfig.ConstellationItem constellationConfig = findConstellationConfig(constellationId, config);
        Long constellationPower = powerCalculator.calculateConstellationPower(saved, constellationConfig);

        log.info("Constellation unlocked: id={}, power={}", constellationId, constellationPower);

        // Update role capability
        updateRoleCapability(String.valueOf(userId), constellationPower, "constellation_unlocked");

        return saved;
    }
    
    @Override
    @Transactional
    public Constellation levelUpConstellation(Long userId, Integer constellationId) {
        log.info("Leveling up constellation for user: {}, id: {}", userId, constellationId);
        
        Constellation constellation = getConstellation(userId, constellationId);
        
        if (!constellation.getIsUnlocked()) {
            throw new StarMapServiceException("Constellation not unlocked", "CONSTELLATION_NOT_UNLOCKED");
        }
        
        if (constellation.getLevel() >= MAX_CONSTELLATION_LEVEL) {
            throw new StarMapServiceException("Constellation at max level", "MAX_CONSTELLATION_LEVEL");
        }
        
        constellation.setLevel(constellation.getLevel() + 1);
        
        return constellationRepository.save(constellation);
    }
    
    @Override
    @Transactional
    public void checkConstellationCompletion(Long userId, Integer constellationId) {
        Constellation constellation = getConstellation(userId, constellationId);
        
        // Count activated stars for this constellation
        long activatedCount = starRepository.countByUserIdAndIsActiveTrue(userId);
        
        constellation.setStarsActivated(activatedCount >= constellation.getTotalStars() 
            ? constellation.getTotalStars() 
            : (int) activatedCount);
        
        if (constellation.getStarsActivated() != null && constellation.getStarsActivated().equals(constellation.getTotalStars())) {
            constellation.setIsCompleted(true);
            log.info("Constellation completed: {}", constellationId);
        }
        constellationRepository.save(constellation);
        
        constellationRepository.save(constellation);
    }
    
    @Override
    public Long calculateTotalStarMapPower(Long userId) {
        List<Star> stars = starRepository.findByUserIdAndIsActiveTrue(userId);
        List<Constellation> constellations = constellationRepository.findByUserIdAndIsUnlockedTrue(userId);

        // Use StarmapPowerCalculator for accurate config-based power calculation
        StarmapConfig config = configProvider.getStarmapConfig();
        return powerCalculator.calculateTotalStarmapPower(stars, constellations, config);
    }

    @Override
    public Long calculateConstellationPower(Constellation constellation) {
        // Use StarmapPowerCalculator for accurate config-based power calculation
        StarmapConfig config = configProvider.getStarmapConfig();
        StarmapConfig.ConstellationItem constellationConfig =
            findConstellationConfig(constellation.getConstellationId(), config);
        return powerCalculator.calculateConstellationPower(constellation, constellationConfig);
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
            .reason(4001) // StarMap system reason code
            .build();
        
        try {
            walletClient.consumeCurrency(playerId, request);
            log.debug("Consumed {} of currency {} for {}", amount, currencyId, reason);
        } catch (Exception e) {
            log.error("Failed to consume currency: {}", e.getMessage());
            throw new StarMapServiceException("Insufficient currency: " + e.getMessage(), "WALLET_ERROR");
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
            throw new StarMapServiceException("Insufficient materials: " + e.getMessage(), "BAG_ERROR");
        }
    }

    private StarmapConfig.StarItem findStarConfig(Integer starId, StarmapConfig config) {
        if (starId == null || config == null || config.getStars() == null) {
            return null;
        }

        return config.getStars().stream()
            .filter(star -> starId.equals(star.getStarId()))
            .findFirst()
            .orElse(null);
    }

    private StarmapConfig.ConstellationItem findConstellationConfig(Integer constellationId, StarmapConfig config) {
        if (constellationId == null || config == null || config.getConstellations() == null) {
            return null;
        }

        return config.getConstellations().stream()
            .filter(constellation -> constellationId.equals(constellation.getConstellationId()))
            .findFirst()
            .orElse(null);
    }

    private void updateRoleCapability(String roleId, Long deltaValue, String reason) {
        if (roleId == null || roleId.isBlank() || deltaValue == null || deltaValue == 0L) {
            return;
        }

        try {
            RoleServiceClient.CapabilityUpdateRequest request =
                new RoleServiceClient.CapabilityUpdateRequest("starmap", deltaValue, reason);
            roleServiceClient.updateCapability(roleId, request);
            log.info("Updated role capability: roleId={}, delta={}, reason={}", roleId, deltaValue, reason);
        } catch (Exception e) {
            log.error("Failed to update role capability: roleId={}, delta={}, error={}",
                roleId, deltaValue, e.getMessage());
        }
    }
}
