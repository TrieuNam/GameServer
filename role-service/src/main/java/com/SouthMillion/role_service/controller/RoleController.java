package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Validated
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService svc;

    @GetMapping("/{roleId}")
    public Optional<RoleDTOs.RoleResp> getById(@PathVariable String roleId) {
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
        return svc.addExp(req.getRoleId(), req.getExp());
    }

    @PostMapping("/{roleId}/rename")
    public RoleDTOs.RoleResp rename(@PathVariable("roleId") String roleId,
                                    @RequestBody @Valid RoleDTOs.RenameReq body) {
        return svc.rename(roleId, body.getName());
    }

    // WX Info set: rename + avatar
    @PostMapping("/{roleId}/wxinfo")
    public RoleDTOs.RoleResp setWxInfo(@PathVariable("roleId") String roleId,
                                       @RequestBody RoleDTOs.WxInfoSetReq body) {
        String name = body != null ? body.name() : null;
        String head = body != null ? body.headChar() : null;
        return svc.setWxInfo(roleId, name, head);
    }
}