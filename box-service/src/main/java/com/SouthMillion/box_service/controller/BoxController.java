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

    @GetMapping("/wabao/book")
    public BoxDTOs.WaBaoBookListInfo wabaoBook(@RequestParam("roleId") Long roleId) {
        return svc.getWaBaoBookListInfo(roleId);
    }
}