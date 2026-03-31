package com.SouthMillion.bag_service.repository;

import com.SouthMillion.bag_service.enity.RecycleProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecycleProgressRepository extends JpaRepository<RecycleProgress, Long> {
    Optional<RecycleProgress> findByRoleId(Long roleId);
}
