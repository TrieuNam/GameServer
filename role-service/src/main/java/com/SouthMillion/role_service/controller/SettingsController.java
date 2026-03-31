package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.RoleSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/role/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final RoleSettingsService svc;

    @PostMapping
    public SettingsDTOs.SystemSetResp apply(@RequestBody @Valid SettingsDTOs.SystemSetReq req) {
        return svc.applySettings(req);
    }
}
