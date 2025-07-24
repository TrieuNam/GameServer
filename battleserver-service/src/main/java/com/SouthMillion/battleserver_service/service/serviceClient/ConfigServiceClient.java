package com.SouthMillion.battleserver_service.service.serviceClient;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "config-service")
public interface ConfigServiceClient {
    @GetMapping("/api/config/monster_group")
    JsonNode getConfigFile();

    @GetMapping("/api/config/monster")
    JsonNode getMonsterConfigFile();

    @GetMapping("/api/config/knights")
    JsonNode getKnightsConfigFile();


    @GetMapping("/api/config/knight_card")
    JsonNode getKnightCardConfigFile();

    @GetMapping("/api/config/pet_item")
    JsonNode getPetItemConfigFile();

    @GetMapping("/api/config/pet_weapon_item")
    JsonNode getPetWeaponItemConfigFile();

    @GetMapping("/api/config/passive_skill")
    JsonNode getPassiveSkillConfigFile();

    @GetMapping("/api/config/single_skill")
    JsonNode getSingleSkillConfigFile();


    // Có thể mở rộng thêm các API khác (knight, skill, ...)
}
