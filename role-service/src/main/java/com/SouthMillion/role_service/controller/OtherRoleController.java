package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.OtherRoleService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/other-role")
@RequiredArgsConstructor
public class OtherRoleController {

    private final OtherRoleService svc;

    @GetMapping("/{uid}")
    public OtherRoleDTOs.OtherRoleInfo getOtherRole(@PathVariable String uid,
                                                    @RequestParam(value = "roleId", required = false) String roleId) {
        return svc.getOtherRole(uid, roleId);
    }
}