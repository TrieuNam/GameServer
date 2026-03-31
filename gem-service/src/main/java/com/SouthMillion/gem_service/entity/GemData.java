package com.SouthMillion.gem_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
/** Gem drawing system (宝石图纸) - tracks gem blueprints/drawings owned by a role */
@Entity @Table(name = "gem_data",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_gem", columnNames = {"role_id", "gem_id"}),
    indexes = @Index(name = "idx_role_id", columnList = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GemData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    /** Gem blueprint/drawing template ID */
    @Column(name = "gem_id", nullable = false)
    private Integer gemId;
    /** Current level of this gem drawing */
    @Column(name = "level", nullable = false)
    private Integer level;
    /** Number owned */
    @Column(name = "count", nullable = false)
    private Integer count;
    /** Whether this gem drawing is inlaid/equipped */
    @Column(name = "is_inlaid", nullable = false)
    private Boolean isInlaid;
    /** Slot position if inlaid (-1 = not inlaid) */
    @Column(name = "slot_type", nullable = false)
    private Integer slotType;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
