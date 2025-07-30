package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.SystemSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Long> {
    Optional<SystemSettingEntity> findByRoleId(Integer roleId);
}