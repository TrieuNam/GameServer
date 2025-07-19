package com.SouthMillion.world_service.service.client;

import org.SouthMillion.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "config-service", url = "http://localhost:8083")
public interface ConfigServiceClient {

    @GetMapping("/api/config/monster")
    List<MonsterDTO> getMonsters();

    @GetMapping("/api/config/monster_group")
    List<MonsterGroupDTO> getGroups();

    @GetMapping("/api/config/passive_skill")
    List<PassiveSkillDTO> getPassiveSkills();

    @GetMapping("/api/config/single_skill")
    List<SingleSkillDTO> getSkills();

    @GetMapping("/api/config/single_skill_effect")
    List<SingleSkillEffectDTO> getSkillEffects();
}
