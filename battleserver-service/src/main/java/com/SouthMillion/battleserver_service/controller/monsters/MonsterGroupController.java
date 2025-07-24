package com.SouthMillion.battleserver_service.controller.monsters;

import com.SouthMillion.battleserver_service.service.monsters.MonsterGroupService;
import org.SouthMillion.dto.battle.MonsterGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/monster_group")
public class MonsterGroupController {
    @Autowired
    private MonsterGroupService monsterGroupService;

    @GetMapping
    public List<MonsterGroupDTO> getMonsterGroups() {
        return monsterGroupService.getAllMonsterGroups();
    }

    @GetMapping("/{id}")
    public MonsterGroupDTO getMonsterGroupById(@PathVariable("id") String monsterGroupId) {
        return monsterGroupService.getMonsterGroupById(monsterGroupId);
    }
}