package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "box-service", path = "/api/box")
public interface BoxFeign {

    @GetMapping("/info")
    BoxDTOs.InfoResp info(@RequestParam("roleId") String roleId);

    @PostMapping("/open")
    BoxDTOs.OpenResp open(@RequestBody BoxDTOs.OpenReq req);

    @PostMapping("/wear")
    BoxDTOs.OkResp wear(@RequestBody BoxDTOs.WearReq req);

    @PostMapping("/sell")
    BoxDTOs.OkResp sell(@RequestBody BoxDTOs.SellReq req);

    @PostMapping("/buy")
    BoxDTOs.OkResp buy(@RequestBody BoxDTOs.SimpleReq req);

    @PostMapping("/level-up")
    BoxDTOs.OkResp levelUp(@RequestBody BoxDTOs.SimpleReq req);

    @PostMapping("/quicken")
    BoxDTOs.OkResp quicken(@RequestBody BoxDTOs.QuickenReq req);

    @PostMapping("/level-reward")
    BoxDTOs.OkResp levelReward(@RequestBody BoxDTOs.LevelRewardReq req);

    @GetMapping("/luck/info")
    BoxDTOs.LuckInfoResp luckInfo(@RequestParam("roleId") String roleId);

    @PostMapping("/luck/receive")
    BoxDTOs.OkResp luckReceive(@RequestBody BoxDTOs.LuckReceiveReq req);
}