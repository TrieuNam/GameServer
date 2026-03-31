package com.SouthMillion.chat_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Muted Player Entity
 */
@Entity
@Table(name = "muted_player", indexes = {
    @Index(name = "idx_mute_role", columnList = "roleId")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MutedPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roleId;

    @Column(nullable = false, length = 50)
    private String roleName;

    @Column(length = 200)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime muteUntil;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime mutedAt;

    public boolean isActive() {
        return LocalDateTime.now().isBefore(muteUntil);
    }
}
