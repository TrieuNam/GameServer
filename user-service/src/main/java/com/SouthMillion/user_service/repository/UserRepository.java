package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.model.User;
import jakarta.transaction.Transactional;
import org.SouthMillion.dto.user.BattleStateDTO;
import org.SouthMillion.dto.user.OnlineStateDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByFacebookId(String facebookId);

}
