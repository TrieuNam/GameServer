package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Weekend HaoLi (周末豪礼) for RandActivity type 30.
 * Weekend premium gift shop activity.
 */
@Entity
@Table(name = "weekend_haoli")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WeekendHaoLi {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Player level (玩家等级) */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** JSON array of purchase times per gift item (购买次数) */
    @Column(name = "buy_times_json", nullable = false, columnDefinition = "TEXT")
    private String buyTimesJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
