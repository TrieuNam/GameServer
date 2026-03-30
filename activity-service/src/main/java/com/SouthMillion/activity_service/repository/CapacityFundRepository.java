package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.CapacityFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CapacityFundRepository extends JpaRepository<CapacityFund, Long> {
    Optional<CapacityFund> findByRoleId(Long roleId);
}
