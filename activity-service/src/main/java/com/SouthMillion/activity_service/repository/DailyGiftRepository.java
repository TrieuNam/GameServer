package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.DailyGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyGiftRepository extends JpaRepository<DailyGift, Long> {
    Optional<DailyGift> findByRoleId(Long roleId);
}
