package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.CustomizedGift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomizedGiftRepository extends JpaRepository<CustomizedGift, Long> {
    Optional<CustomizedGift> findByRoleId(Long roleId);
}
