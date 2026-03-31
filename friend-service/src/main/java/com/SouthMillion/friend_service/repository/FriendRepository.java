package com.SouthMillion.friend_service.repository;

import com.SouthMillion.friend_service.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    /**
     * Find friendship by two role IDs (order independent)
     */
    @Query("SELECT f FROM Friend f WHERE (f.roleId1 = :roleId1 AND f.roleId2 = :roleId2) OR (f.roleId1 = :roleId2 AND f.roleId2 = :roleId1)")
    Optional<Friend> findByRoleIds(@Param("roleId1") Long roleId1, @Param("roleId2") Long roleId2);

    /**
     * Find all friends of a player
     */
    @Query("SELECT f FROM Friend f WHERE f.roleId1 = :roleId OR f.roleId2 = :roleId ORDER BY f.friendshipLevel DESC, f.createdAt DESC")
    List<Friend> findAllFriends(@Param("roleId") Long roleId);

    /**
     * Count friends of a player
     */
    @Query("SELECT COUNT(f) FROM Friend f WHERE f.roleId1 = :roleId OR f.roleId2 = :roleId")
    long countFriends(@Param("roleId") Long roleId);

    /**
     * Check if two players are friends
     */
    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE (f.roleId1 = :roleId1 AND f.roleId2 = :roleId2) OR (f.roleId1 = :roleId2 AND f.roleId2 = :roleId1)")
    boolean areFriends(@Param("roleId1") Long roleId1, @Param("roleId2") Long roleId2);

    /**
     * Find friends by level range
     */
    @Query("SELECT f FROM Friend f WHERE (f.roleId1 = :roleId OR f.roleId2 = :roleId) AND f.friendshipLevel >= :minLevel ORDER BY f.friendshipLevel DESC")
    List<Friend> findFriendsByLevel(@Param("roleId") Long roleId, @Param("minLevel") Integer minLevel);
}
