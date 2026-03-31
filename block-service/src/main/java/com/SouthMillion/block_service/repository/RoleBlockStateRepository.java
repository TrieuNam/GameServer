package com.SouthMillion.block_service.repository;

import com.SouthMillion.block_service.entity.RoleBlockState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleBlockStateRepository extends JpaRepository<RoleBlockState, Long> {
    Optional<RoleBlockState> findByRoleId(Long roleId);
}
