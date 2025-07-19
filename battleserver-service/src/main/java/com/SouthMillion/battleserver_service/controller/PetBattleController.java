package com.SouthMillion.battleserver_service.controller;

import com.SouthMillion.battleserver_service.service.PetBattleService;
import org.SouthMillion.dto.pet.PetBattleResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/battle")
public class PetBattleController {
    @Autowired
    private PetBattleService petBattleService;

    @GetMapping("/pet-vs-pet")
    public PetBattleResultDTO battle(
            @RequestParam int petIdA,
            @RequestParam(required = false) Integer itemA,
            @RequestParam(required = false) Integer weaponA,
            @RequestParam int petIdB,
            @RequestParam(required = false) Integer itemB,
            @RequestParam(required = false) Integer weaponB
    ) {
        return petBattleService.battle(petIdA, itemA, weaponA, petIdB, itemB, weaponB);
    }
}
