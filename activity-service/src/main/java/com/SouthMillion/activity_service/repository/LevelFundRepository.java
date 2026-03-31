package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.LevelFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LevelFundRepository extends JpaRepository<LevelFund, Long> {
    Optional<LevelFund> findByRoleId(Long roleId);
}
