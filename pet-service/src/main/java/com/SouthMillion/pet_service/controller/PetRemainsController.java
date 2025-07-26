package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.PetRemainsService;
import org.SouthMillion.dto.pet.PetRemainsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pet/remains")
public class PetRemainsController {

    @Autowired
    private PetRemainsService petRemainsService;

    @GetMapping("/{userId}")
    public List<PetRemainsDTO> getAllRemains(@PathVariable Long userId) {
        return petRemainsService.getAllRemains(userId);
    }

    @GetMapping("/{userId}/{seq}")
    public PetRemainsDTO getRemains(@PathVariable Long userId, @PathVariable Integer seq) {
        return petRemainsService.getRemains(userId, seq);
    }

    @PostMapping("/{userId}")
    public PetRemainsDTO addRemains(@PathVariable Long userId, @RequestBody PetRemainsDTO dto) {
        return petRemainsService.addRemains(userId, dto);
    }

    @PutMapping("/{userId}")
    public PetRemainsDTO updateRemains(@PathVariable Long userId, @RequestBody PetRemainsDTO dto) {
        return petRemainsService.updateRemains(userId, dto);
    }
}