package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * GuMo Tower Fund (箍魔之塔基金) for RandActivity type 26.
 * Fund tied to GuMo Tower (pagoda) progress.
 */
@Entity
@Table(name = "gumo_tower_fund")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GuMoTowerFund {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** Phase purchase flags (分期购买标识): bitmask for purchase phases */
    @Column(name = "phase_buy_flag", nullable = false)
    private Integer phaseBuyFlag;

    /** Common tier rewards claimed (普通奖励领取标识): bitmask */
    @Column(name = "common_fetch_flag", nullable = false)
    private Long commonFetchFlag;

    /** Senior/Premium tier rewards claimed (高级奖励领取标识): bitmask */
    @Column(name = "senior_fetch_flag", nullable = false)
    private Long seniorFetchFlag;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
