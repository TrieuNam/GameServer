package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.WeekendLianChong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeekendLianChongRepository extends JpaRepository<WeekendLianChong, Long> {
    Optional<WeekendLianChong> findByRoleId(Long roleId);
}
