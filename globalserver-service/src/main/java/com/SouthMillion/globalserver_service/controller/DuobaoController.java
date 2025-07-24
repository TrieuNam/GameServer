package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.DuobaoService;
import org.SouthMillion.dto.globalserver.DuobaoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/duobao")
public class DuobaoController {
    @Autowired
    private DuobaoService duobaoService;

    @GetMapping("/info/{userId}")
    public DuobaoDTO info(@PathVariable Long userId) {
        return duobaoService.getInfo(userId);
    }

    @PostMapping("/draw/{userId}/{opType}/{param1}/{param2}")
    public void draw(@PathVariable Long userId,
                     @PathVariable Integer opType,
                     @PathVariable Integer param1,
                     @PathVariable Integer param2) {
        duobaoService.draw(userId, opType, param1, param2);
    }
}