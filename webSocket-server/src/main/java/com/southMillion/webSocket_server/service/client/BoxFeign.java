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

    @GetMapping("/wabao/collection-book")
    BoxDTOs.WaBaoCollectionBookInfo getWaBaoCollectionBookInfo(@RequestParam("roleId") Long roleId);

    @GetMapping("/wabao/book")
    BoxDTOs.WaBaoBookListInfo getWaBaoBookListInfo(@RequestParam("roleId") Long roleId);

    // ── WaBao CS operations (new endpoints needed) ──
    @PostMapping("/wabao/excavate")
    BoxDTOs.ExcavateResp excavate(@RequestParam("roleId") Long roleId);

    @PostMapping("/wabao/unlock-map")
    BoxDTOs.OkResp unlockMap(@RequestParam("roleId") Long roleId, @RequestParam("mapId") int mapId);

    @PostMapping("/wabao/enter-map")
    BoxDTOs.OkResp enterMap(@RequestParam("roleId") Long roleId, @RequestParam("mapId") int mapId);

    @PostMapping("/wabao/put-collection")
    BoxDTOs.OkResp putCollection(@RequestBody BoxDTOs.PutCollectionReq req);

    @PostMapping("/wabao/collection-sell")
    BoxDTOs.OkResp collectionSell(@RequestBody BoxDTOs.CollectionSellReq req);

    @PostMapping("/wabao/collection-buy")
    BoxDTOs.OkResp collectionBuy(@RequestBody BoxDTOs.SimpleReq req);

    @PostMapping("/wabao/collection-level-up")
    BoxDTOs.OkResp collectionLevelUp(@RequestBody BoxDTOs.SimpleReq req);

    @PostMapping("/wabao/collection-quicken")
    BoxDTOs.OkResp collectionQuicken(@RequestBody BoxDTOs.QuickenReq req);

    @PostMapping("/wabao/fresh-task")
    BoxDTOs.OkResp freshTask(@RequestBody BoxDTOs.FreshTaskReq req);

    @PostMapping("/wabao/fetch-task")
    BoxDTOs.OkResp fetchTask(@RequestBody BoxDTOs.FetchTaskReq req);

    @PostMapping("/wabao/tool-up-level")
    BoxDTOs.OkResp toolUpLevel(@RequestBody BoxDTOs.ToolUpLevelReq req);

    @PostMapping("/wabao/tool-up-grade")
    BoxDTOs.OkResp toolUpGrade(@RequestBody BoxDTOs.ToolUpGradeReq req);

    @PostMapping("/wabao/put-collection-book")
    BoxDTOs.OkResp putCollectionBook(@RequestBody BoxDTOs.PutCollectionBookReq req);

    @PostMapping("/wabao/collection-book-level-up")
    BoxDTOs.OkResp collectionBookLevelUp(@RequestBody BoxDTOs.CollectionBookLevelUpReq req);

    @PostMapping("/wabao/activate-book")
    BoxDTOs.OkResp activateBook(@RequestBody BoxDTOs.ActivateBookReq req);

    @PostMapping("/wabao/fetch-collection-level-reward")
    BoxDTOs.OkResp fetchCollectionLevelReward(@RequestBody BoxDTOs.FetchCollectionLevelRewardReq req);
}