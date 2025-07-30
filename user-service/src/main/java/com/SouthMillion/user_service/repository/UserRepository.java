package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByPlatUserName(String platUserName);
}
