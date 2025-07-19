package com.SouthMillion.battleserver_service.controller.knights;

import com.SouthMillion.battleserver_service.dto.knights.knight.KnightsBookDto;
import com.SouthMillion.battleserver_service.dto.knights.knight.KnightsRewardDto;
import com.SouthMillion.battleserver_service.service.Knights.KnightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/knights")
public class KnightsController {

    @Autowired
    private KnightsService knightsService;

    @GetMapping("/book")
    public List<KnightsBookDto> getAllKnightsBook() {
        return knightsService.getAllKnightsBook();
    }

    @GetMapping("/reward")
    public List<KnightsRewardDto> getAllKnightsReward() {
        return knightsService.getAllKnightsReward();
    }

    @GetMapping("/book/level/{level}/seq/{seq}")
    public KnightsBookDto getKnightsBookByLevelAndSeq(@PathVariable String level, @PathVariable String seq) {
        return knightsService.getKnightsBookByLevelAndSeq(level, seq);
    }

    @GetMapping("/reward/{level}")
    public KnightsRewardDto getKnightsRewardByLevel(@PathVariable String level) {
        return knightsService.getKnightsRewardByLevel(level);
    }
}
