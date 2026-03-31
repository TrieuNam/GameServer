package com.SouthMillion.pagoda_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/** Trial Tower (试炼之塔) progress per role */
@Entity
@Table(name = "shilian_progress",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiLianProgress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "pass_level", nullable = false)
    private Integer passLevel;

    @Column(name = "best_level", nullable = false)
    private Integer bestLevel;

    @Column(name = "use_item", nullable = false)
    private Integer useItem;

    @Column(name = "random_id", nullable = false)
    private Integer randomId;

    @Column(name = "season_end_time", nullable = false)
    private Long seasonEndTime;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
