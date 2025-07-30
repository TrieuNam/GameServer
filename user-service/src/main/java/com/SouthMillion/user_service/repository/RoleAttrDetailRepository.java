package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.RoleAttrDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleAttrDetailRepository extends JpaRepository<RoleAttrDetailEntity, Integer> {
    Optional<RoleAttrDetailEntity> findByRoleId(Integer roleId);
}