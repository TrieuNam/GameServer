package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.RoleSystemSetting;
import com.SouthMillion.role_service.repository.RoleSystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.SouthMillion.dto.role.settings.SystemSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleSettingsService {

    private final RoleSystemSettingRepository repo;

    @Transactional
    public SettingsDTOs.SystemSetResp applySettings(SettingsDTOs.SystemSetReq req) {
        RoleSystemSetting e = repo.findById(req.userId()).orElseGet(() -> {
            RoleSystemSetting r = new RoleSystemSetting();
            r.setUserId(req.userId());
            r.setData(SystemSettings.defaults());
            return r;
        });

        SystemSettings s = e.getData();
        if (s == null) s = SystemSettings.defaults();

        if (req.systemSetList() != null) {
            for (var item : req.systemSetList()) {
                if (item == null || item.key() == null) continue;
                applyOne(s, item.key(), item.value());
            }
        }
        e.setData(s);
        repo.saveAndFlush(e);
        return new SettingsDTOs.SystemSetResp(e.getUserId(), e.getData(), e.getUpdatedAt());
    }

    private void applyOne(SystemSettings s, String keyRaw, Object val) {
        String key = keyRaw.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "music", "musicon" -> s.setMusicOn(asBool(val));
            case "musicvolume", "music_vol", "musicvolumepercent" -> s.setMusicVolume(asInt(val, 0, 100));
            case "sfx", "sfxon" -> s.setSfxOn(asBool(val));
            case "sfxvolume", "sfx_vol", "sfxvolumepercent" -> s.setSfxVolume(asInt(val, 0, 100));
            case "notifications", "notificationson", "push" -> s.setNotificationsOn(asBool(val));
            case "vibrate", "vibrateon" -> s.setVibrateOn(asBool(val));
            case "language", "lang" -> s.setLanguage(val == null ? null : String.valueOf(val));
            default -> s.putExtra(keyRaw, val);
        }
    }

    private boolean asBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        String s = v.toString().trim().toLowerCase(Locale.ROOT);
        return "1".equals(s) || "true".equals(s) || "on".equals(s) || "yes".equals(s);
    }

    private int asInt(Object v, int min, int max) {
        int out;
        if (v instanceof Number n) out = n.intValue();
        else {
            try { out = Integer.parseInt(v.toString().trim()); }
            catch (Exception e) { out = min; }
        }
        if (out < min) out = min;
        if (out > max) out = max;
        return out;
    }
}