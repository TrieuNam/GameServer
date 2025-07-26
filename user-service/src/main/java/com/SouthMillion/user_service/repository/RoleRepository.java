package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByUid(Integer uid);

    Optional<RoleEntity> findByUsername(String pname);
}