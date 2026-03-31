package com.SouthMillion.chat_service.repository;

import com.SouthMillion.chat_service.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChannelOrderByCreatedAtDesc(Integer channel, Pageable pageable);

    List<ChatMessage> findByChannelAndChannelIdOrderByCreatedAtDesc(Integer channel, String channelId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.channel = 4 AND ((m.senderId = :roleId1 AND m.receiverId = :roleId2) OR (m.senderId = :roleId2 AND m.receiverId = :roleId1)) ORDER BY m.createdAt DESC")
    List<ChatMessage> findPrivateChat(@Param("roleId1") String roleId1, @Param("roleId2") String roleId2, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.createdAt < :cutoffTime")
    void deleteOldMessages(@Param("cutoffTime") LocalDateTime cutoffTime);
}
