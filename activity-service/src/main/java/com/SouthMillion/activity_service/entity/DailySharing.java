package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Daily Sharing (每日分享) for RandActivity type 23.
 * Daily sharing reward system.
 */
@Entity
@Table(name = "daily_sharing")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySharing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Number of times rewards fetched today (今日领取次数) */
    @Column(name = "fetch_count", nullable = false)
    private Integer fetchCount;

    /** Last fetch date for daily reset tracking */
    @Column(name = "last_fetch_date")
    private LocalDateTime lastFetchDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
