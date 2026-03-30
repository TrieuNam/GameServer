package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.RuneTowerFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RuneTowerFundRepository extends JpaRepository<RuneTowerFund, Long> {
    Optional<RuneTowerFund> findByRoleId(Long roleId);
}
