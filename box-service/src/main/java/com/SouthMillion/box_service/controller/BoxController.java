package com.SouthMillion.box_service.controller;

import com.SouthMillion.box_service.service.BoxEquipService;
import com.SouthMillion.box_service.service.BoxService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
    public BoxDTOs.InfoResp info(@RequestParam("roleId") String roleId) {
        return svc.info(roleId);
    }

    @PostMapping("/open")
    public BoxDTOs.OpenResp open(@Valid @RequestBody BoxDTOs.OpenReq req) { return svc.open(req); }

    @PostMapping("/wear")
    public BoxDTOs.OkResp wear(@Valid @RequestBody BoxDTOs.WearReq req){ return svc.wear(req.getRoleId()); }

    @PostMapping("/sell")
    public BoxDTOs.OkResp sell(@Valid @RequestBody BoxDTOs.SellReq req){ return svc.sell(req.getRoleId()); }

    @PostMapping("/buy")
    public BoxDTOs.OkResp buy(@Valid @RequestBody BoxDTOs.SimpleReq req){ return svc.buy(req.getRoleId()); }

    @PostMapping("/level-up")
    public BoxDTOs.OkResp levelUp(@Valid @RequestBody BoxDTOs.SimpleReq req){ return svc.levelUp(req.getRoleId()); }

    @PostMapping("/quicken")
    public BoxDTOs.OkResp quicken(@Valid @RequestBody BoxDTOs.QuickenReq req){
        return svc.quicken(req.getRoleId(), req.getNum());
    }

    @PostMapping("/level-reward")
    public BoxDTOs.OkResp levelReward(@Valid @RequestBody BoxDTOs.LevelRewardReq req){
        return svc.levelReward(req.getRoleId(), req.getIdx());
    }

    // Luck Unpacking
    @GetMapping("/luck/info")
    public BoxDTOs.LuckInfoResp luckInfo(@RequestParam("roleId") String roleId) {
        return svc.luckInfo(roleId);
    }

    @PostMapping("/luck/receive")
    public BoxDTOs.OkResp luckReceive(@Valid @RequestBody BoxDTOs.LuckReceiveReq req){
        return svc.luckReceive(req.getRoleId(), req.getSeq());
    }

    @GetMapping("/setting")
    public BoxDTOs.BoxSettingResp getSetting(@RequestParam("roleId")  String roleId) {
        return svc.getSetting(roleId);
    }

    @PostMapping("/setting")
    public BoxDTOs.BoxSettingResp saveSetting(@RequestBody BoxDTOs.BoxSettingReq req) {
        return svc.saveSetting(req.getRoleId(), req.getBoxSet());
    }

    @PostMapping("/decompose")
    public BoxDTOs.DecomposeResp decompose(@RequestParam("roleId")  String roleId) {
        return svc.decompose(roleId);
    }

    @PostMapping("/pending")
    public BoxDTOs.OkResp setPending(@RequestBody BoxDTOs.SetPendingReq req) {
        svc.savePending(req.getRoleId(), req.getPending());
        return BoxDTOs.OkResp.builder().build();
    }

    @GetMapping("/equipInfo")
    public BoxDTOs.EquipInfo equipInfo(@RequestParam("roleId") String roleId) {
        return boxEquipService.getEquipInfo(roleId);
    }
}