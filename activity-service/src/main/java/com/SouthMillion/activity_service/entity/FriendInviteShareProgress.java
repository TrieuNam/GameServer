package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Idempotency guard for friend invite share callback.
 * One inviter/invitee pair should only increase invite count once.
 */
@Entity
@Table(
        name = "friend_invite_share_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inviter_invited_role", columnNames = {"inviter_role_id", "invited_role_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendInviteShareProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inviter_role_id", nullable = false)
    private Long inviterRoleId;

    @Column(name = "invited_role_id", nullable = false)
    private Long invitedRoleId;

    @Column(name = "inviter_user_id")
    private Long inviterUserId;

    @Column(name = "invited_user_id")
    private Long invitedUserId;

    @Column(name = "share_server_id")
    private Integer shareServerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
