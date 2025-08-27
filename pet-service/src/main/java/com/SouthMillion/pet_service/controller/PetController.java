package com.SouthMillion.pet_service.controller;

import com.SouthMillion.pet_service.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.pet.PetDTOs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {
    private final PetService svc;

    @GetMapping("/info")
    public PetDTOs.AllInfoResp info(@RequestParam String roleId) {
        return svc.info(roleId);
    }

    @PostMapping("/set-fight")
    public PetDTOs.OkResp setFight(@RequestBody @Valid PetDTOs.SetFightReq req) {
        return svc.setFight(req);
    }

    @PostMapping("/level-up")
    public PetDTOs.OkResp levelUp(@RequestBody @Valid PetDTOs.LevelUpReq req) {
        return svc.levelUp(req);
    }

    // ===== API khung cho các tính năng tiếp theo (chưa implement) =====
    @PostMapping("/grade-up")  public PetDTOs.OkResp gradeUp()  { return PetDTOs.OkResp.NG("NOT_IMPLEMENTED"); }
    @PostMapping("/gem/inlay") public PetDTOs.OkResp gemInlay() { return PetDTOs.OkResp.NG("NOT_IMPLEMENTED"); }
    @PostMapping("/gem/level-up") public PetDTOs.OkResp gemLevelUp(){ return PetDTOs.OkResp.NG("NOT_IMPLEMENTED"); }
    @PostMapping("/ts-gem/level-up") public PetDTOs.OkResp tsGemLevelUp(){ return PetDTOs.OkResp.NG("NOT_IMPLEMENTED"); }
    @PostMapping("/skill/learn") public PetDTOs.OkResp skillLearn(){ return PetDTOs.OkResp.NG("NOT_IMPLEMENTED"); }
}