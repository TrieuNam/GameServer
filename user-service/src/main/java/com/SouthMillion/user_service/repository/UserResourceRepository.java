package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.UserResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserResourceRepository extends JpaRepository<UserResourceEntity, Long> {
    Optional<UserResourceEntity> findByUserIdAndItemId(Long userId, Integer itemId);
}