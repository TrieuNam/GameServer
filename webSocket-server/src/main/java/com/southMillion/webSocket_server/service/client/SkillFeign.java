package com.SouthMillion.webSocket_server.service.client;

import org.SouthMillion.dto.skill.SkillDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Feign client gọi role-service cho hệ thống kỹ năng (skill) và thiên phú (talent).
 *
 * <p>API:
 * <ul>
 *   <li>GET  /api/skill/{roleId}                   → lấy toàn bộ kỹ năng</li>
 *   <li>POST /api/skill/{roleId}/learn              → học/nâng cấp kỹ năng</li>
 *   <li>POST /api/skill/{roleId}/one-key-level-up   → nâng cấp tất cả kỹ năng</li>
 *   <li>GET  /api/talent/{roleId}                  → lấy toàn bộ thiên phú</li>
 *   <li>POST /api/talent/{roleId}/learn             → học/nâng cấp thiên phú</li>
 * </ul>
 */
@FeignClient(name = "role-service", contextId = "SkillFeign")
public interface SkillFeign {

    // ─────────────────── Skill (kỹ năng chủ động) ───────────────────────

    @GetMapping("/api/skill/{roleId}")
    SkillDTOs.SkillAllInfoResp getSkillAll(@PathVariable("roleId") Long roleId);

    @PostMapping("/api/skill/{roleId}/learn")
    SkillDTOs.SkillAllInfoResp learnSkill(
            @PathVariable("roleId") Long roleId,
            @RequestBody SkillDTOs.LearnSkillReq req);

    @PostMapping("/api/skill/{roleId}/one-key-level-up")
    SkillDTOs.SkillAllInfoResp oneKeyLevelUp(@PathVariable("roleId") Long roleId);

    // ─────────────────── Talent (thiên phú) ─────────────────────────────

    @GetMapping("/api/talent/{roleId}")
    SkillDTOs.TalentAllInfoResp getTalentAll(@PathVariable("roleId") Long roleId);

    @PostMapping("/api/talent/{roleId}/learn")
    SkillDTOs.TalentAllInfoResp learnTalent(
            @PathVariable("roleId") Long roleId,
            @RequestBody SkillDTOs.LearnTalentReq req);
}

