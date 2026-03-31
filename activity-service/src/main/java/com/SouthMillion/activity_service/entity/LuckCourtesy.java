package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Luck Courtesy (幸运礼遇) for RandActivity type 17.
 * Limited-time gift offers with validity tracking.
 */
@Entity
@Table(name = "luck_courtesy")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LuckCourtesy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Open level requirement (开放等级) */
    @Column(name = "open_level", nullable = false)
    private Integer openLevel;

    /** JSON array of gift info: [{isValid, giftSeq, endTimestamp}, ...] */
    @Column(name = "gift_info_json", nullable = false, columnDefinition = "TEXT")
    private String giftInfoJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
