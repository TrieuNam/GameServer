package com.SouthMillion.battleserver_service.service.serviceClient;

import org.SouthMillion.dto.pet.PetStatsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "pet-service")
public interface PetFeignClient {
    @GetMapping("/pet/{petId}/stats")
    PetStatsDTO getPetStats(
            @PathVariable("petId") int petId,
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "weaponId", required = false) Integer weaponId
    );
}