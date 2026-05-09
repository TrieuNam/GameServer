package com.SouthMillion.activity_service.controller;

import com.SouthMillion.activity_service.entity.LoopMine;
import com.SouthMillion.activity_service.service.LoopMineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loopmine")
public class LoopMineController {

    @Autowired
    private LoopMineService loopMineService;

    @GetMapping("/battle-logs")
    public Page<LoopMine> getBattleLogs(@RequestParam Long roleId, 
                                         @RequestParam(defaultValue = "0") int page, 
                                         @RequestParam(defaultValue = "20") int size) {
        return loopMineService.getBattleLogs(roleId, page, size);
    }
}