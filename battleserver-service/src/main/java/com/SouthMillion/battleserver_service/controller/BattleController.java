package com.SouthMillion.battleserver_service.controller;

import com.SouthMillion.battleserver_service.service.serviceProducer.BattleLogicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/battle")
public class BattleController {
    @Autowired
    private BattleLogicService battleLogicService;

    @PostMapping("/end")
    public ResponseEntity<?> endBattle(@RequestParam String battleId, @RequestParam String result) {
        battleLogicService.endBattle(battleId, result);
        return ResponseEntity.ok("Battle ended and event published!");
    }
}