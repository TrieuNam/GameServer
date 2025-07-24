package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUserKey(String userKey);
}
