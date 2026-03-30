package com.SouthMillion.role_service.mapper;

import com.SouthMillion.role_service.entity.Role;
import org.SouthMillion.dto.role.RoleDTOs;

public final class RoleMapper {
    private RoleMapper() {
    }

    public static RoleDTOs.RoleResp toResp(Role r) {
        Long createdSec = (r.getCreatedAt() != null) ? r.getCreatedAt().getEpochSecond() : null;

        return RoleDTOs.RoleResp.builder()
                // Core id / user binding
                .roleId(r.getRoleId() != null ? String.valueOf(r.getRoleId()) : null)
                .userId(r.getUserId())

                // Name priority: emit 'name' là chính; nickname/roleName để null (hoặc duplicate nếu bạn muốn)
                .name(r.getName())
                 .nickname(r.getName())   // nếu muốn “đổ bóng”, mở line này
                 .roleName(r.getName())   // nếu muốn “đổ bóng”, mở line này

                // Level / Exp
                .level(r.getLevel())
                .curExp(r.getExp())   // entity.exp -> DTO.curExp; client có thể đọc alias 'exp' nhờ @JsonAlias

                // RoleInfo fields
                .cap(r.getCap())
                .headPicId(r.getHeadPicId())
                .titleId(r.getTitleId())
                .createTimeEpochSec(createdSec)
                .knightLevel(r.getKnightLevel())
                .headChar(r.getHeadChar())
                .guildName(r.getGuildName())

                // basic stats (optional)
                .hp(r.getHp())
                .attack(r.getAttackValue())
                .defense(r.getDefenseValue())
                .speed(r.getSpeed())
                .build();
    }
}