package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService svc;

    @GetMapping("/{roleId}")
    public Optional<RoleDTOs.RoleResp> getById(@PathVariable Long roleId) {
        return svc.getById(roleId);
    }

    @GetMapping("/by-user/{userId}")
    public List<RoleDTOs.RoleResp> listByUser(@PathVariable String userId) {
        return svc.listByUserId(userId);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public RoleDTOs.RoleResp create(@RequestBody @Valid RoleDTOs.CreateRoleReq req) {
        return svc.create(req);
    }

    @PostMapping("/exp/add")
    public RoleDTOs.RoleResp addExp(@RequestBody @Valid RoleDTOs.AddExpReq req) {
        return svc.addExp(Long.parseLong(req.getRoleId()), req.getExp());
    }

    @PostMapping("/{roleId}/rename")
    public RoleDTOs.RoleResp rename(@PathVariable("roleId") Long roleId,
                                    @RequestBody @Valid RoleDTOs.RenameReq body) {
        return svc.rename(roleId, body.getName());
    }

    // WX Info set: rename + avatar
    @PostMapping("/{roleId}/wxinfo")
    public RoleDTOs.RoleResp setWxInfo(@PathVariable("roleId") Long roleId,
                                       @RequestBody RoleDTOs.WxInfoSetReq body) {
        String name = body != null ? body.name() : null;
        String head = body != null ? body.headChar() : null;
        return svc.setWxInfo(roleId, name, head);
    }

    @PostMapping("/internal/{roleId}/stats/delta")
    public RoleDTOs.RoleResp applyStatDelta(@PathVariable("roleId") Long roleId,
                                            @RequestBody StatDeltaReq req) {
        long hp = req != null ? req.hp() : 0L;
        long atk = req != null ? req.attack() : 0L;
        long def = req != null ? req.defense() : 0L;
        int spd = req != null ? req.speed() : 0;
        return svc.applyStatDelta(roleId, hp, atk, def, spd);
    }

    /**
     * Combat power for battle simulation.
     * Returns { fightPower, hp, atk, def, spd } derived from role level.
     * Real implementation would read from equip/angel/mount stats.
     */
    @GetMapping("/{roleId}/combat-power")
    public Map<String, Object> getCombatPower(@PathVariable Long roleId) {
        return svc.getCombatPower(roleId);
    }

    /**
     * Lightweight role info for service-to-service calls (e.g. arena display names).
     * Returns { name, level, fightPower }.
     */
    @GetMapping("/{roleId}/basic-info")
    public Map<String, Object> getBasicInfo(@PathVariable Long roleId) {
        Optional<RoleDTOs.RoleResp> roleOpt = svc.getById(roleId);
        if (roleOpt.isEmpty()) {
            return Map.of("name", "Player-" + roleId, "level", 1, "fightPower", 0);
        }
        RoleDTOs.RoleResp role = roleOpt.get();
        String displayName = role.getName() != null && !role.getName().isBlank() ? role.getName()
                : role.getNickname() != null ? role.getNickname()
                : role.getRoleName() != null ? role.getRoleName()
                : "Player-" + roleId;
        int level = role.getLevel() != null ? role.getLevel() : 1;
        Map<String, Object> power = svc.getCombatPower(roleId);
        long fightPower = power.get("fightPower") instanceof Number n ? n.longValue() : 0L;
        return Map.of("name", displayName, "level", level, "fightPower", fightPower);
    }

    @PostMapping("/{roleId}/notice-time")
    public Map<String, Object> noticeTime(@PathVariable("roleId") Long roleId,
                                          @RequestBody NoticeTimeReq req) {
        int type = req != null ? req.type() : 0;
        long param = req != null ? req.param() : 0L;
        return Map.of("noticeTime", svc.noticeTime(roleId, type, param));
    }

    public record StatDeltaReq(long hp, long attack, long defense, int speed) {}
    public record NoticeTimeReq(int type, long param) {}
}