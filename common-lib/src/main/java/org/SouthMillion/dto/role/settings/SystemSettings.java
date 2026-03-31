package org.SouthMillion.dto.role.settings;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {
    private Boolean musicOn;
    private Integer musicVolume;
    private Boolean sfxOn;
    private Integer sfxVolume;
    private Boolean notificationsOn;
    private Boolean vibrateOn;
    private String language;
    private Map<String, Object> extras;

    public static SystemSettings defaults() {
        SystemSettings s = new SystemSettings();
        s.setMusicOn(true);
        s.setMusicVolume(100);
        s.setSfxOn(true);
        s.setSfxVolume(100);
        s.setNotificationsOn(true);
        s.setVibrateOn(true);
        s.setLanguage("vi");
        s.setExtras(new HashMap<>());
        return s;
    }

    public void putExtra(String key, Object value) {
        if (extras == null) extras = new HashMap<>();
        extras.put(key, value);
    }
}