package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.TerritoryGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerritoryGiftRepository extends JpaRepository<TerritoryGift, Long> {
    Optional<TerritoryGift> findByRoleId(Long roleId);
}
