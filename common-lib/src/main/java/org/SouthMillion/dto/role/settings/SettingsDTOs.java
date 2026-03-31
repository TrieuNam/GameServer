package org.SouthMillion.dto.role.settings;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class SettingsDTOs {

    public record SystemSettingItem(@NotBlank String key, Object value) {}
    public record SystemSetReq(@NotBlank String userId, List<SystemSettingItem> systemSetList) {}
    public record SystemSetResp(String userId, SystemSettings settings, Instant updatedAt) {}
}