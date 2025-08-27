package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.shop.ResultDTO;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "shop-service", path = "/api/shop")
public interface ShopFeign {

    @PostMapping("/buy")
    ResultDTO<ShopDTOs.BuyResp> buy(@RequestBody ShopDTOs.BuyReq req);

    // (tuỳ chọn) nếu cần bootstrap/gỡ rối
    @GetMapping("/list/common")
    ResultDTO<ShopDTOs.ShopListResp> listCommon(@RequestParam("level") int level,
                                                @RequestParam("page") int page);

    @PostMapping("/list/cloth")
    ResultDTO<ShopDTOs.ShopListResp> listCloth(@RequestBody ShopDTOs.ListClothReq req);

    @GetMapping("/list/mystery")
    ResultDTO<ShopDTOs.ShopListResp> listMystery(@RequestParam("level") int level,
                                                 @RequestParam(value = "slots", required = false) Integer slots);


    @GetMapping("/info")
     ResultDTO<ShopDTOs.InfoResp> info(@RequestParam("roleId") String roleId,
                                             @RequestParam(value = "level", required = false) Integer level,
                                             @RequestParam(value = "slots", required = false) Integer slots) ;
}