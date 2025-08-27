package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.RoleService;
import jakarta.validation.Valid;
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
    public RoleDTOs.ListResp list(@RequestParam(name = "userId") String userId) {
        return svc.listByUser(userId);
    }

    @GetMapping("/{roleId}")
    public RoleDTOs.RoleResp detail(@PathVariable("roleId") String roleId) {
        return svc.detail(roleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDTOs.RoleResp create(@RequestBody @Valid RoleDTOs.CreateRoleReq req) {
        return svc.create(req);
    }

    @PostMapping("/exp/add")
    public void addExp(@RequestBody RoleDTOs.AddExpReq req) {
        svc.addExp(req.getRoleId(), req.getExp()); // tự bạn hiện thực
    }

    @PostMapping("/{roleId}/rename")
    public RoleDTOs.RoleResp rename(@PathVariable("roleId") String roleId,
                                    @RequestBody @Valid RoleDTOs.RenameReq body) {
        return svc.rename(roleId, body.getName());
    }
}