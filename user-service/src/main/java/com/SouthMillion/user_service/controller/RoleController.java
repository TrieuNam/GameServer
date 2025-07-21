package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.RoleService;
import org.SouthMillion.dto.user.RoleAttrDto;
import org.SouthMillion.dto.user.RoleDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @GetMapping("/roleInfo")
    public RoleDto getRoleInfo(@RequestParam String roleId) {
        return roleService.getRoleInfo(roleId);
    }

    @GetMapping("/roleAttrs")
    public List<RoleAttrDto> getRoleAttrs(@RequestParam String roleId) {
        return roleService.getRoleAttrs(roleId);
    }

    @GetMapping("/roleExp")
    public Long getRoleExp(@RequestParam String roleId) {
        return roleService.getExp(roleId);
    }

    @GetMapping("/roleLevel")
    public Integer getRoleLevel(@RequestParam String roleId) {
        return roleService.getLevel(roleId);
    }
}
