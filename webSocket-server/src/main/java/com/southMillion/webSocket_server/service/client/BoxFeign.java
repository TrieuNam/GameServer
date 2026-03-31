package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "box-service", path = "/api/box")
public interface BoxFeign {

    @GetMapping("/info")
    BoxDTOs.InfoResp info(@RequestParam("roleId") Long roleId);

    @PostMapping("/open")
    BoxDTOs.OpenResp open(@RequestBody BoxDTOs.OpenReq req);

    @PostMapping("/wear")
    BoxDTOs.OkResp wear(@RequestBody BoxDTOs.WearReq req);

    @PostMapping("/sell")
    BoxDTOs.SellResp sell(@RequestBody BoxDTOs.SellReq req);

    @PostMapping("/buy")
    BoxDTOs.OkResp buy(@RequestBody BoxDTOs.SimpleReq req);

    @PostMapping("/level-up")
    BoxDTOs.OkResp levelUp(@RequestBody BoxDTOs.SimpleReq req);

    @PostMapping("/quicken")
    BoxDTOs.OkResp quicken(@RequestBody BoxDTOs.QuickenReq req);

    @PostMapping("/level-reward")
    BoxDTOs.OkResp levelReward(@RequestBody BoxDTOs.LevelRewardReq req);

    @GetMapping("/luck/info")
    BoxDTOs.LuckInfoResp luckInfo(@RequestParam("roleId") Long roleId);

    @PostMapping("/luck/receive")
    BoxDTOs.OkResp luckReceive(@RequestBody BoxDTOs.LuckReceiveReq req);


    @PostMapping("/decompose")
    BoxDTOs.DecomposeResp decompose(@RequestParam("roleId") Long roleId);


    @GetMapping("/setting")
    BoxDTOs.BoxSettingResp getSetting(@RequestParam("roleId") Long roleId);

    /** Save auto-sweep stop conditions — maps to BoxController POST /setting */
    @PostMapping("/setting")
    BoxDTOs.BoxSettingResp saveSetting(@RequestBody BoxDTOs.BoxSettingReq req);

    @GetMapping("/compare-state")
    BoxDTOs.BoxCompareStateResp getCompareState(@RequestParam("roleId") Long roleId);

    @DeleteMapping("/compare-state")
    BoxDTOs.OkResp clearCompareState(@RequestParam("roleId") Long roleId);

    @GetMapping("/equipInfo")
    BoxDTOs.EquipInfo equipInfo(@RequestParam("roleId") Long roleId);

    // ── WaBao SC data (SC 1643/1645/1646/1647/1648/1651) ──
    @GetMapping("/wabao/map")
    BoxDTOs.WaBaoMapInfo getWaBaoMapInfo(@RequestParam("roleId") Long roleId);

    @GetMapping("/wabao/integrity")
    BoxDTOs.WaBaoIntegrityInfo getWaBaoIntegrity(@RequestParam("roleId") Long roleId);

    @GetMapping("/wabao/collection")
    BoxDTOs.WaBaoCollectionInfo getWaBaoCollection(@RequestParam("roleId") Long roleId);

    @GetMapping("/wabao/tool")
    BoxDTOs.WaBaoToolInfo getWaBaoToolInfo(@RequestParam("roleId") Long roleId);

    @GetMapping("/wabao/task")
    BoxDTOs.WaBaoTaskInfo getWaBaoTaskInfo(@RequestParam("roleId") Long roleId);

    @GetMapping("/wabao/book")
    BoxDTOs.WaBaoBookListInfo getWaBaoBookListInfo(@RequestParam("roleId") Long roleId);
}