package org.SouthMillion.dto.user;

import lombok.Data;

@Data
public class UserSettingsDto {
    private String userKey;
    private String settingsJson;
}
