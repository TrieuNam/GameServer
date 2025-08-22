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