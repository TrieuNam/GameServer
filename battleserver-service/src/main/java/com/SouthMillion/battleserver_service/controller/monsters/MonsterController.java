package com.SouthMillion.battleserver_service.controller.monsters;

import com.SouthMillion.battleserver_service.service.monsters.MonsterService;
import org.SouthMillion.dto.MonsterDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/monster")
public class MonsterController {

    @Autowired
    private MonsterService monsterService;

    @GetMapping
    public List<MonsterDTO> getAllMonsters() {
        return monsterService.getAllMonsters();
    }

    @GetMapping("/{id}")
    public MonsterDTO getMonsterById(@PathVariable("id") String id) {
        return monsterService.getMonsterById(id);
    }
}