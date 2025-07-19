package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.PetService;
import org.SouthMillion.dto.pet.PetDTO;
import org.SouthMillion.dto.pet.PetItemDTO;
import org.SouthMillion.dto.pet.PetStatsDTO;
import org.SouthMillion.dto.pet.PetWeaponDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pet")
public class PetController {
    @Autowired
    private PetService petService;

    @GetMapping("/{petId}")
    public ResponseEntity<PetDTO> getPetById(@PathVariable int petId) {
        PetDTO pet = petService.getPetById(petId);
        if (pet == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(pet);
    }

    @GetMapping
    public List<PetDTO> getAllPets() {
        return petService.getAllPets();
    }

    @GetMapping("/{petId}/stats")
    public PetStatsDTO getPetStats(
            @PathVariable int petId,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(required = false) Integer weaponId
    ) {
        return petService.calcPetStats(petId, itemId, weaponId);
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<PetItemDTO> getPetItemById(@PathVariable int itemId) {
        PetItemDTO item = petService.getPetItemById(itemId);
        if (item == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(item);
    }

    @GetMapping("/item")
    public List<PetItemDTO> getAllPetItems() {
        return petService.getAllPetItems();
    }

    @GetMapping("/weapon/{weaponId}")
    public ResponseEntity<PetWeaponDTO> getPetWeaponById(@PathVariable int weaponId) {
        PetWeaponDTO weapon = petService.getPetWeaponById(weaponId);
        if (weapon == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(weapon);
    }

    @GetMapping("/weapon")
    public List<PetWeaponDTO> getAllPetWeapons() {
        return petService.getAllPetWeapons();
    }

    @PostMapping("/{petId}/level-up")
    public boolean levelUpPet(@PathVariable int petId, @RequestParam int exp) { return petService.levelUpPet(petId, exp); }
    @PostMapping("/{petId}/advance")
    public boolean advancePet(@PathVariable int petId) { return petService.advancePet(petId); }
    @PostMapping("/{petId}/apply-buff")
    public void applyBuff(@PathVariable int petId, @RequestParam int skillId) { petService.applyBuff(petService.getPetById(petId), skillId); }
    @GetMapping("/{petId}/roll-skill")
    public int rollRandomSkill(@PathVariable int petId) { return petService.rollRandomSkill(petId); }
    @PostMapping("/use-skill")
    public void useSkill(@RequestParam int petId, @RequestParam int skillId, @RequestParam int targetPetId) {
        petService.useSkill(petService.getPetById(petId), skillId, petService.getPetById(targetPetId));
    }
    @PostMapping("/boss-fight")
    public String fightBoss(@RequestParam int playerPetId, @RequestParam int bossPetId) {
        return petService.fightBoss(petService.getPetById(playerPetId), petService.getPetById(bossPetId));
    }
    @PostMapping("/team-battle")
    public String teamBattle(@RequestBody List<Integer> teamA, @RequestBody List<Integer> teamB) {
        List<PetDTO> a = teamA.stream().map(petService::getPetById).toList();
        List<PetDTO> b = teamB.stream().map(petService::getPetById).toList();
        return petService.teamBattle(a, b);
    }
}