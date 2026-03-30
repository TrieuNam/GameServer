package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OtherRoleService {

    private final RoleRepository roleRepo;

    @Transactional(readOnly = true)
    public OtherRoleDTOs.OtherRoleInfo getOtherRole(String uid, String roleIdOpt) {
        Role r = (roleIdOpt != null && !roleIdOpt.isBlank())
                ? roleRepo.findById(Long.valueOf(roleIdOpt)).orElseThrow(() -> new IllegalArgumentException("Role not found"))
                : roleRepo.findFirstByUserIdOrderByCreatedAtAsc(uid).orElseThrow(() -> new IllegalArgumentException("Role not found for uid"));

        var attr = new OtherRoleDTOs.OtherRoleAttr(r.getLevel(), r.getExp(), r.getHp(), r.getAttackValue(), r.getDefenseValue(), r.getSpeed());
        return new OtherRoleDTOs.OtherRoleInfo(
                r.getUserId(), String.valueOf(r.getRoleId()), r.getName(), r.getHeadChar(), r.getGuildName(),
                attr, Collections.emptyList(), Collections.emptyList(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}