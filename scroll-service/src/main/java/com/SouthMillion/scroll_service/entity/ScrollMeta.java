package com.SouthMillion.scroll_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
/** Scroll system metadata per role: free draw count and guarantee counter */
@Entity @Table(name = "scroll_meta",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ScrollMeta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;
    /** Number of free draws available today */
    @Column(name = "free_num", nullable = false)
    private Integer freeNum;
    /** Guarantee counter (pity system) */
    @Column(name = "bao_di_num", nullable = false)
    private Integer baoDiNum;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
