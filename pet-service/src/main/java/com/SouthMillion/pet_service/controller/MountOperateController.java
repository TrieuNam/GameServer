package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.MountOperateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mount")
@RequiredArgsConstructor
public class MountOperateController {

    private final MountOperateService mountOperateService;

    /** Nâng cấp mount */
    @PostMapping("/levelup")
    public String levelUp(
            @RequestParam String playerId,
            @RequestParam Integer mountId
    ) {
        mountOperateService.levelUpMount(playerId, mountId);
        return "OK";
    }

    /** Nâng bậc mount */
    @PostMapping("/gradeup")
    public String gradeUp(
            @RequestParam String playerId,
            @RequestParam Integer mountId
    ) {
        mountOperateService.gradeUpMount(playerId, mountId);
        return "OK";
    }

    /** Trang bị harness (马具) */
    @PostMapping("/wear")
    public String wearHarness(
            @RequestParam String playerId,
            @RequestParam Integer harnessId
    ) {
        mountOperateService.wearHarness(playerId, harnessId);
        return "OK";
    }

    /** Phân giải harness */
    @PostMapping("/decompose")
    public String decomposeHarness(
            @RequestParam String playerId,
            @RequestParam Integer harnessId
    ) {
        mountOperateService.decomposeHarness(playerId, harnessId);
        return "OK";
    }

    /** Tẩy luyện thuộc tính harness */
    @PostMapping("/refresh-entry")
    public String refreshEntry(
            @RequestParam String playerId,
            @RequestParam Integer harnessId,
            @RequestParam Integer lockFlag,
            @RequestParam Integer consumeItemId
    ) {
        mountOperateService.refreshEntry(playerId, harnessId, lockFlag, consumeItemId);
        return "OK";
    }
}