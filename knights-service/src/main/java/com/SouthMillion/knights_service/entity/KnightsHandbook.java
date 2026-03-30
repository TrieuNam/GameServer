package com.SouthMillion.knights_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Tracks knight handbook progress per role.
 * level = handbook level reached
 * flag = bitmask of claimed sequential rewards
 * levelFlag = bitmask of claimed level rewards
 */
@Entity
@Table(name = "knights_handbook",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KnightsHandbook {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Handbook level */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** Bitmask of claimed sequential rewards */
    @Column(name = "flag", nullable = false)
    private Long flag;

    /** Bitmask of claimed level rewards */
    @Column(name = "level_flag", nullable = false)
    private Long levelFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
