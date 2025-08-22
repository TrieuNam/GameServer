package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.PetService;
import org.SouthMillion.dto.pet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pet")
public class PetController {
    @Autowired
    private PetService petService;

    @GetMapping("/{roleId}/all-info")
    public ResponseEntity<PetAllInfoDTO> getPetAllInfo(@PathVariable String roleId) {
        PetAllInfoDTO info = petService.getPetAllInfo(roleId);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetConfigDTO.PetDTO> getPetById(@PathVariable int petId) {
        PetConfigDTO.PetDTO pet = petService.getPetById(petId);
        if (pet == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(pet);
    }

    @GetMapping
    public List<PetConfigDTO.PetDTO> getAllPets() {
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

    @GetMapping("/{roleId}/{petIndex}")
    public ResponseEntity<PetDataDTO> getPetData(@PathVariable String roleId, @PathVariable int petIndex) {
        PetDataDTO pet = petService.getPetData(roleId, petIndex);
        return ResponseEntity.ok(pet);
    }

    @GetMapping("/{roleId}/ts-gem/{gemIndex}")
    public ResponseEntity<PetTSGemDataDTO> getTSGemData(@PathVariable String roleId, @PathVariable int gemIndex) {
        PetTSGemDataDTO gem = petService.getTSGemData(roleId, gemIndex);
        return ResponseEntity.ok(gem);
    }

    @GetMapping("/{roleId}/cloth-list")
    public List<PetClothDataDTO> getClothList(@PathVariable String roleId) {
        return petService.getClothList(roleId);
    }

    @PostMapping("/{roleId}/pet-op")
    public PetOpResultDTO petOperate(@PathVariable String roleId, @RequestBody PetOpRequestDTO req) {
        return petService.petOperate(roleId, req);
    }

    @PostMapping("/{roleId}/one-key-up-gem")
    public PetGemOpResultDTO oneKeyUpLevelGem(@PathVariable String roleId, @RequestBody List<PetOneKeyGemInfoDTO> items) {
        return petService.oneKeyUpLevelGem(roleId, items);
    }

    @GetMapping("/{roleId}/evo-attr/{petIndex}")
    public PetEvoAttrDTO getPetEvoAttr(@PathVariable String roleId, @PathVariable int petIndex) {
        return petService.getPetEvoAttr(roleId, petIndex);
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
        List<PetConfigDTO.PetDTO> a = teamA.stream().map(petService::getPetById).toList();
        List<PetConfigDTO.PetDTO> b = teamB.stream().map(petService::getPetById).toList();
        return petService.teamBattle(a, b);
    }
}