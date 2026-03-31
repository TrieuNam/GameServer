package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.RoleSystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleSystemSettingRepository extends JpaRepository<RoleSystemSetting, String> {
}