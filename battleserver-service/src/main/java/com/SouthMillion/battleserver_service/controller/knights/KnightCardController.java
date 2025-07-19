package com.SouthMillion.battleserver_service.controller.knights;

import com.SouthMillion.battleserver_service.dto.knights.knight_card.KnightCardDto;
import com.SouthMillion.battleserver_service.dto.knights.knight_card.KnightZhengDto;
import com.SouthMillion.battleserver_service.service.Knights.KnightCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/battle/knight_card")
public class KnightCardController {

    @Autowired
    private KnightCardService knightCardService;

    @GetMapping
    public List<KnightCardDto> getAllKnightCards() {
        return knightCardService.getAllKnightCards();
    }

    @GetMapping("/zheng")
    public List<KnightZhengDto> getAllKnightZhengs() {
        return knightCardService.getAllKnightZhengs();
    }

    @GetMapping("/zheng/{seq}")
    public KnightZhengDto getKnightZhengBySeq(@PathVariable String seq) {
        return knightCardService.getKnightZhengBySeq(seq);
    }
}
