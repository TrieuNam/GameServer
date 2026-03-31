package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.RechargeInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RechargeInfoRepository extends JpaRepository<RechargeInfo, Long> {
    Optional<RechargeInfo> findByRoleId(Long roleId);
}
