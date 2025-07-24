package com.SouthMillion.world_service.controller;

import com.SouthMillion.world_service.service.MonsterCache;
import com.SouthMillion.world_service.service.client.BattleServiceClient;
import lombok.Data;
import org.SouthMillion.dto.battle.BattleRequestDTO;
import org.SouthMillion.dto.battle.BattleResultDTO;
import org.SouthMillion.dto.battle.MonsterInstanceDTO;
import org.SouthMillion.dto.battle.MonsterKilledRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/world")
public class WorldController {

    @Autowired
    private MonsterCache monsterCache;
    @Autowired private BattleServiceClient battleFeign;

    @GetMapping("/monsters")
    public List<MonsterInstanceDTO> getMonsters() {
        return monsterCache.getAllAlive();
    }

    @PostMapping("/encounter")
    public BattleResultDTO encounter(@RequestBody EncounterRequest req) {
        MonsterInstanceDTO monster = monsterCache.getAliveInstance(req.getMonsterInstanceId());
        if (monster == null) throw new RuntimeException("Monster dead");
        BattleRequestDTO br = new BattleRequestDTO();
        br.setPlayerId(req.getPlayerId());
        br.setPlayerAttack(req.getPlayerAttack());
        br.setPlayerDefense(req.getPlayerDefense());
        br.setPlayerSpeed(req.getPlayerSpeed());
        br.setPlayerSkillIds(req.getPlayerSkillIds());
        br.setPlayerPassiveSkillIds(req.getPlayerPassiveSkillIds());
        br.setMonsters(List.of(monster));
        return battleFeign.fight(br);
    }

    @PostMapping("/monster-killed")
    public void monsterKilled(@RequestBody MonsterKilledRequestDTO req) {
        monsterCache.setDead(req.getMonsterInstanceId());
    }

    @Data
    public static class EncounterRequest {
        private String playerId;
        private String monsterInstanceId;
        private int playerAttack, playerDefense, playerSpeed;
        private List<String> playerSkillIds;
        private List<String> playerPassiveSkillIds;
    }
}