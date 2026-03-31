package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.DailySharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailySharingRepository extends JpaRepository<DailySharing, Long> {
    Optional<DailySharing> findByRoleId(Long roleId);
}
