package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.UserItemEntity;
import com.SouthMillion.user_service.enity.UserItemKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItemEntity, UserItemKey> {
    Optional<UserItemEntity> findById(UserItemKey id);
}