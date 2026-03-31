package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Friend Invite (好友邀请) for RandActivity type 20.
 * Friend invitation milestone reward system.
 */
@Entity
@Table(name = "friend_invite")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FriendInvite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Number of friends successfully invited (邀请数量) */
    @Column(name = "invite_count", nullable = false)
    private Integer inviteCount;

    /** Milestone rewards claimed (奖励领取标识): bitmask */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
