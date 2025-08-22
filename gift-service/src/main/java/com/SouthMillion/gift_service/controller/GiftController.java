package com.SouthMillion.gift_service.controller;

import com.SouthMillion.gift_service.service.GiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.gift.GiftDTOs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gift")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService svc;

    /**
     * Preview thông tin 1 gift (để client hiện tool-tip)
     */
    @GetMapping("/{giftItemId}/info")
    public GiftDTOs.GiftInfoResp info(@PathVariable long giftItemId) {
        return svc.info(giftItemId);
    }

    /**
     * Mở gift: tiêu thụ hộp trong bag, roll và cộng item/coin
     */
    @PostMapping("/open")
    public GiftDTOs.OpenResp open(@Valid @RequestBody GiftDTOs.OpenReq req) {
        return svc.open(req);
    }
}