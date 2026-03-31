package com.SouthMillion.pagoda_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/** Demon-Lock Tower (锢魔之塔) layer progress per role */
@Entity
@Table(name = "gumo_layer",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_layer", columnNames = {"role_id", "layer_id"}),
    indexes = @Index(name = "idx_role_id", columnList = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GuMoLayer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "layer_id", nullable = false)
    private Integer layerId;

    /** Star rating achieved (0-3) */
    @Column(name = "star_flag", nullable = false)
    private Integer starFlag;

    /** Whether reward box has been claimed */
    @Column(name = "box_flag", nullable = false)
    private Boolean boxFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
