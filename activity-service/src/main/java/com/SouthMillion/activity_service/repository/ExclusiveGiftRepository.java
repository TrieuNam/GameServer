package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.ExclusiveGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExclusiveGiftRepository extends JpaRepository<ExclusiveGift, Long> {
    Optional<ExclusiveGift> findByRoleId(Long roleId);
}
