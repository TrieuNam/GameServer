package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.ChestManor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChestManorRepository extends JpaRepository<ChestManor, Long> {
    Optional<ChestManor> findByRoleId(Long roleId);
}
