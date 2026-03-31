package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.skill.SkillDTOs;
import org.springframework.web.bind.annotation.*;

/**
 * REST API cho hệ thống kỹ năng (skill) và thiên phú (talent) của nhân vật.
 *
 * <p>Được gọi từ webSocket-server qua SkillFeign.
 *
 * <pre>
 * GET  /api/skill/{roleId}                    → danh sách kỹ năng
 * POST /api/skill/{roleId}/learn              → học/nâng cấp kỹ năng
 * POST /api/skill/{roleId}/one-key-level-up   → nâng cấp tất cả kỹ năng
 * GET  /api/talent/{roleId}                   → danh sách thiên phú
 * POST /api/talent/{roleId}/learn             → học/nâng cấp thiên phú
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    // ═══════════════════════════════ SKILL ═══════════════════════════════

    /** Lấy toàn bộ kỹ năng của nhân vật. */
    @GetMapping("/api/skill/{roleId}")
    public SkillDTOs.SkillAllInfoResp getSkillAll(@PathVariable Long roleId) {
        return skillService.getSkillAll(roleId);
    }

    /**
     * Học hoặc nâng cấp một kỹ năng.
     *
     * @param roleId  nhân vật
     * @param req     {@code skillId} — id kỹ năng cần học/nâng
     */
    @PostMapping("/api/skill/{roleId}/learn")
    public SkillDTOs.SkillAllInfoResp learnSkill(
            @PathVariable Long roleId,
            @RequestBody(required = false) SkillDTOs.LearnSkillReq req) {
        Integer skillId = req != null ? req.getSkillId() : null;
        return skillService.learnSkill(roleId, skillId);
    }

    /** Nâng cấp tất cả kỹ năng lên 1 cấp (one-key level up). */
    @PostMapping("/api/skill/{roleId}/one-key-level-up")
    public SkillDTOs.SkillAllInfoResp oneKeyLevelUp(@PathVariable Long roleId) {
        return skillService.oneKeyLevelUp(roleId);
    }

    // ═══════════════════════════════ TALENT ═════════════════════════════=

    /** Lấy toàn bộ thiên phú của nhân vật. */
    @GetMapping("/api/talent/{roleId}")
    public SkillDTOs.TalentAllInfoResp getTalentAll(@PathVariable Long roleId) {
        return skillService.getTalentAll(roleId);
    }

    /**
     * Học hoặc nâng cấp một thiên phú.
     *
     * @param roleId  nhân vật
     * @param req     {@code skillId} — id thiên phú cần học/nâng
     */
    @PostMapping("/api/talent/{roleId}/learn")
    public SkillDTOs.TalentAllInfoResp learnTalent(
            @PathVariable Long roleId,
            @RequestBody(required = false) SkillDTOs.LearnTalentReq req) {
        Integer skillId = req != null ? req.getSkillId() : null;
        return skillService.learnTalent(roleId, skillId);
    }
}
