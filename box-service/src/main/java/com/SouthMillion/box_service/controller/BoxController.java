package com.SouthMillion.box_service.controller;

import com.SouthMillion.box_service.service.BoxEquipService;
import com.SouthMillion.box_service.service.BoxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/box")
@RequiredArgsConstructor
public class BoxController {
    private final BoxService svc;
    private final BoxEquipService boxEquipService;

    @GetMapping("/info")
    public BoxDTOs.InfoResp info(@RequestParam("roleId") Long roleId) {
        return svc.info(roleId);
    }

    @PostMapping("/open")
    public BoxDTOs.OpenResp open(@Valid @RequestBody BoxDTOs.OpenReq req) { return svc.open(req); }

    @PostMapping("/wear")
    public BoxDTOs.OkResp wear(@Valid @RequestBody BoxDTOs.WearReq req) {
        return svc.wear(Long.valueOf(req.getRoleId()));
    }

    @PostMapping("/sell")
    public BoxDTOs.SellResp sell(@Valid @RequestBody BoxDTOs.SellReq req) {
        return svc.sell(Long.valueOf(req.getRoleId()));
    }

    @PostMapping("/buy")
    public BoxDTOs.OkResp buy(@Valid @RequestBody BoxDTOs.SimpleReq req) {
        return svc.buy(Long.valueOf(req.getRoleId()));
    }

    @PostMapping("/level-up")
    public BoxDTOs.OkResp levelUp(@Valid @RequestBody BoxDTOs.SimpleReq req) {
        return svc.levelUp(Long.valueOf(req.getRoleId()));
    }

    @PostMapping("/quicken")
    public BoxDTOs.OkResp quicken(@Valid @RequestBody BoxDTOs.QuickenReq req) {
        return svc.quicken(Long.valueOf(req.getRoleId()), req.getNum());
    }

    @PostMapping("/level-reward")
    public BoxDTOs.OkResp levelReward(@Valid @RequestBody BoxDTOs.LevelRewardReq req) {
        return svc.levelReward(Long.valueOf(req.getRoleId()), req.getIdx());
    }

    // Luck Unpacking
    @GetMapping("/luck/info")
    public BoxDTOs.LuckInfoResp luckInfo(@RequestParam("roleId") Long roleId) {
        return svc.luckInfo(roleId);
    }

    @PostMapping("/luck/receive")
    public BoxDTOs.OkResp luckReceive(@Valid @RequestBody BoxDTOs.LuckReceiveReq req) {
        return svc.luckReceive(Long.valueOf(req.getRoleId()), req.getSeq());
    }

    @GetMapping("/setting")
    public BoxDTOs.BoxSettingResp getSetting(@RequestParam("roleId") Long roleId) {
        return svc.getSetting(roleId);
    }

    @PostMapping("/setting")
    public BoxDTOs.BoxSettingResp saveSetting(@RequestBody BoxDTOs.BoxSettingReq req) {
        return svc.saveSetting(Long.valueOf(req.getRoleId()), req.getBoxSet());
    }

    @PostMapping("/decompose")
    public BoxDTOs.DecomposeResp decompose(@RequestParam("roleId") Long roleId) {
        return svc.decompose(roleId);
    }

    @GetMapping("/compare-state")
    public BoxDTOs.BoxCompareStateResp getCompareState(@RequestParam("roleId") Long roleId) {
        return svc.getCompareState(roleId);
    }

    @DeleteMapping("/compare-state")
    public BoxDTOs.OkResp clearCompareState(@RequestParam("roleId") Long roleId) {
        svc.clearCompareState(roleId);
        return BoxDTOs.OkResp.builder().ok(true).message("OK").build();
    }

    @GetMapping("/equipInfo")
    public BoxDTOs.EquipInfo equipInfo(@RequestParam("roleId") Long roleId) {
        return boxEquipService.getEquipInfo(roleId);
    }

    // ── WaBao SC data endpoints (SC 1643/1645/1646/1647/1648/1650/1651) ──
    @GetMapping("/wabao/map")
    public BoxDTOs.WaBaoMapInfo wabaoMapInfo(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoMapInfo(roleId);
    }

    @GetMapping("/wabao/integrity")
    public BoxDTOs.WaBaoIntegrityInfo wabaoIntegrity(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoIntegrity(roleId);
    }

    @GetMapping("/wabao/collection")
    public BoxDTOs.WaBaoCollectionInfo wabaoCollection(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoCollection(roleId);
    }

    @GetMapping("/wabao/tool")
    public BoxDTOs.WaBaoToolInfo wabaoTool(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoToolInfo(roleId);
    }

    @GetMapping("/wabao/task")
    public BoxDTOs.WaBaoTaskInfo wabaoTask(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoTaskInfo(roleId);
    }

    @GetMapping("/wabao/collection-book")
    public BoxDTOs.WaBaoCollectionBookInfo wabaoCollectionBook(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoCollectionBookInfo(roleId);
    }

    @GetMapping("/wabao/book")
    public BoxDTOs.WaBaoBookListInfo wabaoBook(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoBookListInfo(roleId);
    }

    // ── WaBao CS Operation Endpoints ──

    /** Op 3: excavate/wa_bao (core gacha operation) */
    @PostMapping("/wabao/excavate")
    public BoxDTOs.ExcavateResp excavate(@RequestParam("roleId") Long roleId) {
        return svc.excavate(roleId);
    }

    /** Op 1: unlock map */
    @PostMapping("/wabao/unlock-map")
    public BoxDTOs.OkResp unlockMap(@RequestParam("roleId") Long roleId, @RequestParam("mapId") int mapId) {
        return svc.unlockMap(roleId, mapId);
    }

    /** Op 2: enter map */
    @PostMapping("/wabao/enter-map")
    public BoxDTOs.OkResp enterMap(@RequestParam("roleId") Long roleId, @RequestParam("mapId") int mapId) {
        return svc.enterMap(roleId, mapId);
    }

    /** Op 5: put collection (place item in collection cabinet) */
    @PostMapping("/wabao/put-collection")
    public BoxDTOs.OkResp putCollection(@Valid @RequestBody BoxDTOs.PutCollectionReq req) {
        return svc.putCollection(req);
    }

    /** Op 6: collection sell (remove and sell collection item) */
    @PostMapping("/wabao/collection-sell")
    public BoxDTOs.OkResp collectionSell(@Valid @RequestBody BoxDTOs.CollectionSellReq req) {
        return svc.collectionSell(req);
    }

    /** Op 7: buy collection upgrade */
    @PostMapping("/wabao/collection-buy")
    public BoxDTOs.OkResp collectionBuy(@Valid @RequestBody BoxDTOs.SimpleReq req) {
        return svc.collectionBuy(req);
    }

    /** Op 8: collection level up */
    @PostMapping("/wabao/collection-level-up")
    public BoxDTOs.OkResp collectionLevelUp(@Valid @RequestBody BoxDTOs.SimpleReq req) {
        return svc.collectionLevelUp(req);
    }

    /** Op 9: collection quicken */
    @PostMapping("/wabao/collection-quicken")
    public BoxDTOs.OkResp collectionQuicken(@Valid @RequestBody BoxDTOs.QuickenReq req) {
        return svc.collectionQuicken(req);
    }

    /** Op 10: refresh task (get new daily task) */
    @PostMapping("/wabao/fresh-task")
    public BoxDTOs.OkResp freshTask(@Valid @RequestBody BoxDTOs.FreshTaskReq req) {
        return svc.freshTask(req);
    }

    /** Op 11: fetch task reward (claim task completion reward) */
    @PostMapping("/wabao/fetch-task")
    public BoxDTOs.OkResp fetchTask(@Valid @RequestBody BoxDTOs.FetchTaskReq req) {
        return svc.fetchTask(req);
    }

    /** Op 12: tool level up */
    @PostMapping("/wabao/tool-up-level")
    public BoxDTOs.OkResp toolUpLevel(@Valid @RequestBody BoxDTOs.ToolUpLevelReq req) {
        return svc.toolUpLevel(req);
    }

    /** Op 13: tool grade up */
    @PostMapping("/wabao/tool-up-grade")
    public BoxDTOs.OkResp toolUpGrade(@Valid @RequestBody BoxDTOs.ToolUpGradeReq req) {
        return svc.toolUpGrade(req);
    }

    /** Op 14: put item in collection book */
    @PostMapping("/wabao/put-collection-book")
    public BoxDTOs.OkResp putCollectionBook(@Valid @RequestBody BoxDTOs.PutCollectionBookReq req) {
        return svc.putCollectionBook(req);
    }

    /** Op 15: collection book level up */
    @PostMapping("/wabao/collection-book-level-up")
    public BoxDTOs.OkResp collectionBookLevelUp(@Valid @RequestBody BoxDTOs.CollectionBookLevelUpReq req) {
        return svc.collectionBookLevelUp(req);
    }

    /** Op 16: activate book */
    @PostMapping("/wabao/activate-book")
    public BoxDTOs.OkResp activateBook(@Valid @RequestBody BoxDTOs.ActivateBookReq req) {
        return svc.activateBook(req);
    }

    /** Op 17: fetch collection level reward */
    @PostMapping("/wabao/fetch-collection-level-reward")
    public BoxDTOs.OkResp fetchCollectionLevelReward(@Valid @RequestBody BoxDTOs.FetchCollectionLevelRewardReq req) {
        return svc.fetchCollectionLevelReward(req);
    }
}
