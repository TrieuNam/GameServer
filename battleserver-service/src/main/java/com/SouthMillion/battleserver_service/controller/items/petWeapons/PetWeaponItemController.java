package com.SouthMillion.battleserver_service.controller.items.petWeapons;

import com.SouthMillion.battleserver_service.dto.items.petWeapons.PetWeaponItemDto;
import com.SouthMillion.battleserver_service.service.items.petWeapons.PetWeaponItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/pet_weapon_item")
public class PetWeaponItemController {

    @Autowired
    private PetWeaponItemService petWeaponItemService;

    @GetMapping
    public List<PetWeaponItemDto> getAllPetWeaponItems() {
        return petWeaponItemService.getAllPetWeaponItems();
    }

    @GetMapping("/{id}")
    public PetWeaponItemDto getPetWeaponItemById(@PathVariable String id) {
        return petWeaponItemService.getPetWeaponItemById(id);
    }
}
