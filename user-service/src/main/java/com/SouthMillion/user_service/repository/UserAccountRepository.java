package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.enity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findBySpidAndExternalId(String spid, String externalId);
    boolean existsByUsername(String username);
}