package com.SouthMillion.pagoda_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/** GuMo tower metadata per role (day reward, last day level) */
@Entity
@Table(name = "gumo_meta",
    uniqueConstraints = @UniqueConstraint(name = "uq_role_id", columnNames = "role_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GuMoMeta {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    @Column(name = "day_reward", nullable = false)
    private Integer dayReward;

    @Column(name = "lastday_level", nullable = false)
    private Integer lastDayLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
