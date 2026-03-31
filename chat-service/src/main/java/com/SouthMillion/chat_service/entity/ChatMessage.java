package com.SouthMillion.chat_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Chat Message Entity
 * 
 * Channels:
 * 1 = World
 * 2 = Guild
 * 3 = Team
 * 4 = Private
 * 5 = System
 */
@Entity
@Table(name = "chat_message", indexes = {
    @Index(name = "idx_msg_channel", columnList = "channel"),
    @Index(name = "idx_msg_sender", columnList = "senderId"),
    @Index(name = "idx_msg_receiver", columnList = "receiverId"),
    @Index(name = "idx_msg_time", columnList = "createdAt")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer channel; // 1=World, 2=Guild, 3=Team, 4=Private, 5=System

    @Column(nullable = false, length = 50)
    private String senderId;

    @Column(nullable = false, length = 50)
    private String senderName;

    @Column(length = 50)
    private String receiverId; // For private chat

    @Column(length = 50)
    private String receiverName;

    @Column(length = 50)
    private String channelId; // Guild ID or Team ID

    @Column(nullable = false, length = 500)
    private String content;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
