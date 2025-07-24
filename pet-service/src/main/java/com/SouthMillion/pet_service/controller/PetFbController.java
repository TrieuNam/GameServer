package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.PetFbService;
import org.SouthMillion.dto.globalserver.PetFbDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/petfb")
public class PetFbController {
    @Autowired
    private PetFbService petFbService;

    @GetMapping("/info/{userId}")
    public PetFbDTO info(@PathVariable Long userId) {
        return petFbService.getInfo(userId);
    }

    @PostMapping("/operate/{userId}/{type}/{level}")
    public void operate(@PathVariable Long userId,
                        @PathVariable Integer type,
                        @PathVariable Integer level) {
        petFbService.operate(userId, type, level);
    }
}
