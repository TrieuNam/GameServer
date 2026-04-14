package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.model.entity.PetGuardState;
import com.SouthMillion.pet_service.service.PetGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/petguard")
@RequiredArgsConstructor
@Validated
public class PetGuardController {
    private final PetGuardService petGuardService;

    @GetMapping("/state/{roleId}")
    public PetGuardState getState(@PathVariable Long roleId) {
        return petGuardService.getOrInitState(roleId);
    }

    @PostMapping("/state")
    public void saveState(@Valid @RequestBody PetGuardState state) {
        petGuardService.saveState(state);
    }
}
