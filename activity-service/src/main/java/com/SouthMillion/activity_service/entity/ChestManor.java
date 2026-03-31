package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Chest Manor (宝箱庄园) for RandActivity type 21.
 * Manor chest shop with purchase tracking.
 */
@Entity
@Table(name = "chest_manor")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChestManor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Open level requirement (开放等级) */
    @Column(name = "open_level", nullable = false)
    private Integer openLevel;

    /** JSON array of purchase times per chest type (购买次数) */
    @Column(name = "buy_times_json", nullable = false, columnDefinition = "TEXT")
    private String buyTimesJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
