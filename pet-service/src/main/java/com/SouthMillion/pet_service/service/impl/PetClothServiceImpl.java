package com.SouthMillion.pet_service.service.impl;

import com.SouthMillion.pet_service.client.BagClient;
import com.SouthMillion.pet_service.client.WalletClient;
import com.SouthMillion.pet_service.exception.PetServiceException;
import com.SouthMillion.pet_service.model.entity.Pet;
import com.SouthMillion.pet_service.model.entity.PetCloth;
import com.SouthMillion.pet_service.repository.PetClothRepository;
import com.SouthMillion.pet_service.repository.PetRepository;
import com.SouthMillion.pet_service.service.PetClothService;
import com.SouthMillion.pet_service.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pet Cloth Service Implementation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetClothServiceImpl implements PetClothService {

    private final PetClothRepository clothRepository;
    private final PetRepository petRepository;
    private final PetService petService;
    private final WalletClient walletClient;
    private final BagClient bagClient;

    @Override
    @Transactional
    public void clothUp(String userId, Integer clothId, boolean isDiamond) {
        log.info("Upgrading clothing: userId={}, clothId={}, isDiamond={}", 
            userId, clothId, isDiamond);

        PetCloth cloth = getOrCreateCloth(userId, clothId);

        // Calculate upgrade cost based on current level
        int currentLevel = cloth.getLevel();
        long diamondCost = (currentLevel + 1) * 500L; // Progressive: 500, 1000, 1500...
        int clothItemId = 7001 + (clothId % 10); // Cloth material items: 7001-7010
        int clothItemCount = (currentLevel / 5) + 1; // 1-20 items based on level

        if (isDiamond) {
            // Consume diamonds via wallet-service
            consumeDiamonds(userId, diamondCost, "pet_cloth_upgrade");
            log.debug("Consumed {} diamonds for cloth upgrade", diamondCost);
        } else {
            // Consume cloth items via bag-service
            consumeMaterial(userId, clothItemId, clothItemCount);
            log.debug("Consumed cloth items: itemId={}, count={}", clothItemId, clothItemCount);
        }

        cloth.setLevel(cloth.getLevel() + 1);
        clothRepository.save(cloth);

        // Update pet capability if worn
        if (cloth.getPetIndex() != 0) {
            Pet pet = petService.getPet(userId, cloth.getPetIndex());
            pet.setCapability(petService.calculateCapability(userId, cloth.getPetIndex()));
            petRepository.save(pet);
        }

        log.info("Clothing upgraded: userId={}, clothId={}, newLevel={}", 
            userId, clothId, cloth.getLevel());
    }

    @Override
    @Transactional
    public void clothWear(String userId, Integer petIndex, Integer clothId) {
        log.info("Wearing clothing: userId={}, petIndex={}, clothId={}", 
            userId, petIndex, clothId);

        Pet pet = petService.getPet(userId, petIndex);
        PetCloth cloth = getCloth(userId, clothId);

        if (cloth.getLevel() == 0) {
            throw new PetServiceException("Clothing must be upgraded before wearing");
        }

        // Remove from old pet if equipped elsewhere
        if (cloth.getPetIndex() != 0 && !cloth.getPetIndex().equals(petIndex)) {
            Pet oldPet = petService.getPet(userId, cloth.getPetIndex());
            oldPet.setClothId(0);
            oldPet.setCapability(petService.calculateCapability(userId, cloth.getPetIndex()));
            petRepository.save(oldPet);
        }

        // Remove old cloth from current pet
        if (pet.getClothId() != 0 && !pet.getClothId().equals(clothId)) {
            Optional<PetCloth> oldCloth = clothRepository.findByUserIdAndClothId(userId, pet.getClothId());
            oldCloth.ifPresent(c -> {
                c.setPetIndex(0);
                clothRepository.save(c);
            });
        }

        // Equip new cloth
        pet.setClothId(clothId);
        pet.setCapability(petService.calculateCapability(userId, petIndex));
        petRepository.save(pet);

        cloth.setPetIndex(petIndex);
        clothRepository.save(cloth);

        log.info("Clothing worn: userId={}, petIndex={}, clothId={}", 
            userId, petIndex, clothId);
    }

    @Override
    public PetCloth getCloth(String userId, Integer clothId) {
        return clothRepository.findByUserIdAndClothId(userId, clothId)
            .orElseThrow(() -> new PetServiceException(
                "Clothing not found: userId=" + userId + ", clothId=" + clothId));
    }

    @Override
    @Transactional
    public PetCloth getOrCreateCloth(String userId, Integer clothId) {
        Optional<PetCloth> existing = clothRepository.findByUserIdAndClothId(userId, clothId);
        
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new cloth with level 0
        PetCloth cloth = new PetCloth();
        cloth.setUserId(userId);
        cloth.setClothId(clothId);
        cloth.setLevel(0);
        cloth.setPetIndex(0);
        
        return clothRepository.save(cloth);
    }

    @Override
    public Long calculateClothBonus(String userId, Integer clothId) {
        if (clothId == null || clothId == 0) {
            return 0L;
        }

        try {
            PetCloth cloth = getCloth(userId, clothId);
            
            // Calculate bonus based on cloth level
            // Formula: level * 200 (base) + level^2 * 10 (scaling)
            long baseBonus = cloth.getLevel() * 200L;
            long scalingBonus = (long) Math.pow(cloth.getLevel(), 2) * 10;
            return baseBonus + scalingBonus;
        } catch (PetServiceException e) {
            return 0L;
        }
    }

    @Override
    @Transactional
    public void unequipCloth(String userId, Integer petIndex) {
        log.info("Unequipping cloth: userId={}, petIndex={}", userId, petIndex);

        Pet pet = petService.getPet(userId, petIndex);
        
        if (pet.getClothId() != 0) {
            Optional<PetCloth> cloth = clothRepository.findByUserIdAndClothId(userId, pet.getClothId());
            cloth.ifPresent(c -> {
                c.setPetIndex(0);
                clothRepository.save(c);
            });

            pet.setClothId(0);
            pet.setCapability(petService.calculateCapability(userId, petIndex));
            petRepository.save(pet);
        }
    }
    
    /**
     * Helper: Consume diamonds from wallet
     */
    private void consumeDiamonds(String userId, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        
        List<WalletDTOs.Change> changes = new ArrayList<>();
        changes.add(WalletDTOs.Change.builder()
            .itemId(3L) // Diamonds
            .amount(amount)
            .build());
        
        WalletDTOs.BatchReq request = WalletDTOs.BatchReq.builder()
            .roleId(userId.toString())
            .changes(changes)
            .idemKey(UUID.randomUUID().toString())
            .reason(5001) // Pet cloth reason code
            .build();
        
        try {
            walletClient.consumeCurrency(userId.toString(), request);
            log.debug("Consumed {} diamonds for {}", amount, reason);
        } catch (Exception e) {
            log.error("Failed to consume diamonds: {}", e.getMessage());
            throw new PetServiceException("Insufficient diamonds: " + e.getMessage());
        }
    }
    
    /**
     * Helper: Consume material from bag
     */
    private void consumeMaterial(String userId, int itemId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        
        BagDTOs.UseItemReq request = new BagDTOs.UseItemReq();
        request.setItemId(itemId);
        request.setNum(quantity);
        
        try {
            bagClient.useItem(userId.toString(), request);
            log.debug("Consumed material: itemId={}, quantity={}", itemId, quantity);
        } catch (Exception e) {
            log.error("Failed to consume material: {}", e.getMessage());
            throw new PetServiceException("Insufficient materials: " + e.getMessage());
        }
    }
}
