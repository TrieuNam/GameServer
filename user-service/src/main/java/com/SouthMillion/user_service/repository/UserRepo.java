package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByAccount(String account);
}