package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.RoleDTOs;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService svc;

    // WebSocket đang gọi: GET /api/role/list?userId=...
    @GetMapping("/list")
    public RoleDTOs.ListResp list(@RequestParam("userId") String userId) {
        return svc.listByUser(userId);
    }

    @GetMapping("/{roleId}")
    public RoleDTOs.RoleResp detail(@PathVariable String roleId) {
        return svc.detail(roleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDTOs.RoleResp create(@RequestBody @Validated RoleDTOs.CreateRoleReq req) {
        return svc.create(req);
    }

    @PostMapping("/{roleId}/exp/add")
    public RoleDTOs.RoleResp addExp(@PathVariable String roleId,
                                    @RequestBody @Validated RoleDTOs.AddExpReq body) {
        return svc.addExp(roleId, body.getAddExp());
    }

    @PostMapping("/{roleId}/rename")
    public RoleDTOs.RoleResp rename(@PathVariable String roleId,
                                    @RequestBody @Validated RoleDTOs.RenameReq body) {
        return svc.rename(roleId, body.getName());
    }
}