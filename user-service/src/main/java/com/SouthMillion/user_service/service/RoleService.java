package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.model.Role;
import com.SouthMillion.user_service.model.RoleAttr;
import com.SouthMillion.user_service.repository.RoleAttrRepository;
import com.SouthMillion.user_service.repository.RoleRepository;
import lombok.AllArgsConstructor;
import org.SouthMillion.dto.user.RoleAttrDto;
import org.SouthMillion.dto.user.RoleDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleAttrRepository roleAttrRepository;

    public RoleDto getRoleInfo(String roleId) {
        Optional<Role> roleOpt = roleRepository.findByRoleId(roleId);
        if (!roleOpt.isPresent()) return null;
        Role role = roleOpt.get();

        RoleDto dto = new RoleDto();
        dto.setRoleId(role.getRoleId());
        dto.setRoleName(role.getRoleName());
        dto.setServerId(role.getServerId());
        dto.setLevel(role.getLevel());
        dto.setVip(role.getVip());
        dto.setLastLoginTime(role.getLastLoginTime());
        dto.setCreateTime(role.getCreateTime());
        dto.setExp(role.getExp());
        dto.setCap(role.getCap());

        List<RoleAttrDto> attrDtos = role.getAttrs() != null ?
                role.getAttrs().stream().map(attr -> {
                    RoleAttrDto a = new RoleAttrDto();
                    a.setAttrType(attr.getAttrType());
                    a.setAttrValue(attr.getAttrValue());
                    return a;
                }).collect(Collectors.toList()) : null;
        dto.setAttrs(attrDtos);

        return dto;
    }

    public List<RoleAttrDto> getRoleAttrs(String roleId) {
        List<RoleAttr> attrs = roleAttrRepository.findByRole_RoleId(roleId);
        return attrs.stream().map(attr -> {
            RoleAttrDto dto = new RoleAttrDto();
            dto.setAttrType(attr.getAttrType());
            dto.setAttrValue(attr.getAttrValue());
            return dto;
        }).collect(Collectors.toList());
    }

    public Long getExp(String roleId) {
        return roleRepository.findByRoleId(roleId).map(Role::getExp).orElse(0L);
    }

    public Integer getLevel(String roleId) {
        return roleRepository.findByRoleId(roleId).map(Role::getLevel).orElse(1);
    }
}