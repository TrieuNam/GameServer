package com.SouthMillion.role_service.repository;

import com.SouthMillion.role_service.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findByUserIdOrderByRoleIdAsc(String userId);
    Optional<Role> findByUserIdAndName(String userId, String name);
}