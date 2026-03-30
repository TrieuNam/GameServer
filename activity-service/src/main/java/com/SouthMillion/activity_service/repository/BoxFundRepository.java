package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.BoxFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoxFundRepository extends JpaRepository<BoxFund, Long> {
    Optional<BoxFund> findByRoleId(Long roleId);
}
