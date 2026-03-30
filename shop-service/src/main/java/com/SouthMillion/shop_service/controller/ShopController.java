package com.SouthMillion.shop_service.controller;

import com.SouthMillion.shop_service.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService svc;


    /**
     * Bootstrap shop info for a role:
     * - Mystery shop items (randomized per request)
     * - Slots & next reset epoch (midnight in configured TZ)
     *
     * Optional query:
     * - level: override role level (fallback: query role-service)
     * - slots: override number of mystery slots (fallback: app.shenmi.default-slots)
     */
    @GetMapping("/info")
    public ResultDTO<ShopDTOs.InfoResp> info(@RequestParam("roleId") Long roleId,
                                             @RequestParam(value = "level", required = false) Integer level,
                                             @RequestParam(value = "slots", required = false) Integer slots) {
        return svc.info(String.valueOf(roleId), level, slots);
    }

    @PostMapping("/list/common")
    public ResultDTO<ShopDTOs.ShopListResp> listCommon(@Valid @RequestBody ShopDTOs.ListCommonReq req) {
        return svc.listCommon(req);
    }

    @PostMapping("/list/cloth")
    public ResultDTO<ShopDTOs.ShopListResp> listCloth(@Valid @RequestBody ShopDTOs.ListClothReq req) {
        return svc.listCloth(req);
    }

    @GetMapping("/list/mystery")
    public ResultDTO<ShopDTOs.ShopListResp> listMystery(@RequestParam int level,
                                                        @RequestParam(required = false) Integer slots) {
        return svc.listMystery(level, slots);
    }

    @PostMapping("/buy")
    public ResultDTO<ShopDTOs.BuyResp> buy(@Valid @RequestBody ShopDTOs.BuyReq req) {
        return svc.buy(req);
    }
}