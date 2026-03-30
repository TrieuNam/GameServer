package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.WeekendRecharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeekendRechargeRepository extends JpaRepository<WeekendRecharge, Long> {
    Optional<WeekendRecharge> findByRoleId(Long roleId);
}
