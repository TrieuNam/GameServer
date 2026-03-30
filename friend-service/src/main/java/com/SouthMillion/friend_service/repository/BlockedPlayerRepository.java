package com.SouthMillion.friend_service.repository;

import com.SouthMillion.friend_service.entity.BlockedPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedPlayerRepository extends JpaRepository<BlockedPlayer, Long> {

    /**
     * Find block relationship
     */
    Optional<BlockedPlayer> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * Find all blocked players by a player
     */
    List<BlockedPlayer> findByBlockerIdOrderByBlockedAtDesc(Long blockerId);

    /**
     * Check if player is blocked
     */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * Count blocked players
     */
    long countByBlockerId(Long blockerId);

    /**
     * Delete block relationship
     */
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
