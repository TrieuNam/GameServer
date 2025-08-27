package com.southMillion.webSocket_server.service.client;
import org.SouthMillion.dto.gift.GiftDTOs;
import org.SouthMillion.dto.role.ResultDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "gift-service", path = "/api/gift")
public interface GiftFeign {

    @GetMapping("/{giftItemId}/info")
    GiftDTOs.GiftInfoResp info(@PathVariable("giftItemId") long giftItemId);

    @PostMapping("/open")
    GiftDTOs.OpenResp open(@RequestBody GiftDTOs.OpenReq req);

    /** Gọi nhanh: giftFeign.use(roleId, itemId, num, null) */
    @PostMapping("/use")
    ResultDTO<GiftDTOs.OpenResp> use(@RequestParam("roleId") String roleId,
                                     @RequestParam("itemId") long giftItemId,
                                     @RequestParam("num") int num,
                                     @RequestParam(value = "bagType", required = false) Integer bagType);

}