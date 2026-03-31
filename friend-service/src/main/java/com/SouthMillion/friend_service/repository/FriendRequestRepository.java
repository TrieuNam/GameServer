package com.SouthMillion.friend_service.repository;

import com.SouthMillion.friend_service.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    /**
     * Find request by sender and receiver
     */
    Optional<FriendRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    /**
     * Find pending request
     */
    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, Integer status);

    /**
     * Find all sent requests by a player
     */
    List<FriendRequest> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    /**
     * Find all received requests by a player
     */
    List<FriendRequest> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    /**
     * Find pending requests received by a player
     */
    List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, Integer status);

    /**
     * Count pending requests for a player
     */
    long countByReceiverIdAndStatus(Long receiverId, Integer status);

    /**
     * Check if pending request exists
     */
    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, Integer status);

    /**
     * Delete old processed requests
     */
    @Modifying
    @Query("DELETE FROM FriendRequest r WHERE r.status != 0 AND r.processedAt < :cutoffTime")
    void deleteOldProcessedRequests(@Param("cutoffTime") LocalDateTime cutoffTime);
}
