package com.SouthMillion.friend_service.repository;

import com.SouthMillion.friend_service.entity.OnlineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineStatusRepository extends JpaRepository<OnlineStatus, Long> {

    /**
     * Find status by role ID
     */
    Optional<OnlineStatus> findByRoleId(Long roleId);

    /**
     * Find all online players
     */
    List<OnlineStatus> findByOnlineTrue();

    /**
     * Check if player is online
     */
    boolean existsByRoleIdAndOnlineTrue(Long roleId);

    /**
     * Count online players
     */
    long countByOnlineTrue();
    List<OnlineStatus> findByRoleNameContainingIgnoreCase(String keyword);
}
