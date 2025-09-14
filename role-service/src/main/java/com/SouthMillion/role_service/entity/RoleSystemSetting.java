package com.SouthMillion.role_service.entity;

import com.SouthMillion.role_service.utils.SystemSettingsConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.role.settings.SystemSettings;

import java.time.Instant;

@Entity
@Table(name = "role_system_setting")
@Getter
@Setter
public class RoleSystemSetting {

    @Id
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Convert(converter = SystemSettingsConverter.class)
    @Column(name = "data", columnDefinition = "json", nullable = false)
    private SystemSettings data;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
