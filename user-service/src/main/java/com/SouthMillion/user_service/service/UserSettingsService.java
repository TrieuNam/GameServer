package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.model.UserSettings;
import com.SouthMillion.user_service.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.UserSettingsDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingsService {
    private final UserSettingsRepository repository;

    public void updateSettings(UserSettingsDto dto) {
        UserSettings settings = repository.findByUserKey(dto.getUserKey())
                .orElse(new UserSettings());
        settings.setUserKey(dto.getUserKey());
        settings.setSettingsJson(dto.getSettingsJson());
        repository.save(settings);
    }

    public UserSettingsDto getSettings(String userKey) {
        return repository.findByUserKey(userKey)
                .map(s -> {
                    UserSettingsDto dto = new UserSettingsDto();
                    dto.setUserKey(s.getUserKey());
                    dto.setSettingsJson(s.getSettingsJson());
                    return dto;
                })
                .orElse(null);
    }
}