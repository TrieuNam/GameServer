package com.SouthMillion.crafting_service.repository;

import com.SouthMillion.crafting_service.entity.UserCrafting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCraftingRepository extends JpaRepository<UserCrafting, Long> {
    List<UserCrafting> findByRoleId(String roleId);
    List<UserCrafting> findByRoleIdAndStatus(String roleId, String status);
    Optional<UserCrafting> findFirstByRoleIdAndStatusOrderByIdDesc(String roleId, String status);
    Optional<UserCrafting> findLatestByRoleIdAndStatus(String roleId, String status);
}
