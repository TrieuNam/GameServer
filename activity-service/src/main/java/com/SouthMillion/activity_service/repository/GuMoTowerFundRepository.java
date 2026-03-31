package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.GuMoTowerFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuMoTowerFundRepository extends JpaRepository<GuMoTowerFund, Long> {
    Optional<GuMoTowerFund> findByRoleId(Long roleId);
}
