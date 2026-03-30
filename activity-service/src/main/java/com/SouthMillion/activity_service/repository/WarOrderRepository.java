package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.WarOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarOrderRepository extends JpaRepository<WarOrder, Long> {
    Optional<WarOrder> findByRoleId(Long roleId);
}
