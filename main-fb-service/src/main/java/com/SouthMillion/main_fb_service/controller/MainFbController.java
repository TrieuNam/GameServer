package com.SouthMillion.main_fb_service.controller;

import com.SouthMillion.main_fb_service.service.MainFbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mainfb")
@RequiredArgsConstructor
public class MainFbController {
    private final MainFbService svc;

    @GetMapping("/progress/{playerId}")
    public MainFbDTOs.ProgressResp getProgress(@PathVariable String playerId) {
        return svc.getProgress(playerId);
    }

    // NEW: nhiệm vụ hiện tại (lấy từ maoxian.json/clearance + DB)
    @GetMapping("/task/{playerId}")
    public TaskResp getTask(@PathVariable String playerId) {
        return svc.getCurrentTask(playerId);
    }

    @PostMapping("/enter")
    public MainFbDTOs.EnterStageResp enter(@RequestBody MainFbDTOs.EnterStageReq req) {
        return svc.enter(req);
    }

    @PostMapping("/finish")
    public List<ItemStackDTO> finish(@RequestBody MainFbDTOs.FinishStageReq req) {
        return svc.finish(req);
    }

    @PostMapping("/sweep")
    public MainFbDTOs.SweepResp sweep(@RequestBody MainFbDTOs.SweepReq req) {
        return svc.sweep(req);
    }

    @PostMapping("/chapter/{stage}/claim")
    public List<ItemStackDTO> claim(@PathVariable int stage,
                                    @RequestParam String playerId,
                                    @RequestParam String chapterLabel) {
        return svc.claimChapterReward(playerId, stage, chapterLabel);
    }
}