package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.TianxuanGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TianxuanGiftRepository extends JpaRepository<TianxuanGift, Long> {
    Optional<TianxuanGift> findByRoleId(Long roleId);
}
