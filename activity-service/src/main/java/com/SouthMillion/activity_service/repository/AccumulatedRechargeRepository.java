package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.AccumulatedRecharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccumulatedRechargeRepository extends JpaRepository<AccumulatedRecharge, Long> {
    Optional<AccumulatedRecharge> findByRoleId(Long roleId);
}
