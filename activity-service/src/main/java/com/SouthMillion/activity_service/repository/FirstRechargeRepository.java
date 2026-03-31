package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.FirstRecharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FirstRechargeRepository extends JpaRepository<FirstRecharge, Long> {
    Optional<FirstRecharge> findByRoleId(Long roleId);
}
