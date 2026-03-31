package com.SouthMillion.role_service.entity;

import com.SouthMillion.role_service.utils.IdGen;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ad_reward_claim",
        uniqueConstraints = @UniqueConstraint(name = "uk_ad_claim_user_seq_day", columnNames = {"user_id", "seq", "claim_day"}))
@Getter
@Setter
public class AdRewardClaim {

    @Id
    @Column(name = "id", nullable = false, length = 26)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Column(name = "is_diamond", nullable = false)
    private boolean diamond;

    @Column(name = "param")
    private Integer param;

    @Column(name = "claim_day", nullable = false)
    private LocalDate claimDay;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = IdGen.ulid();
        if (claimedAt == null) claimedAt = Instant.now();
    }
}