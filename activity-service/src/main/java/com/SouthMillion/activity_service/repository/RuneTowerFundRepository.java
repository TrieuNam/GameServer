package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.RuneTowerFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;

@Repository
public interface RuneTowerFundRepository extends JpaRepository<RuneTowerFund, Long> {
    @QueryHints(@QueryHint(name = "javax.persistence.query.timeout", value = "5000"))
    Optional<RuneTowerFund> findByRoleId(Long roleId);
}