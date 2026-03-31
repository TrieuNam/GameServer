package com.SouthMillion.main_fb_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "main_fb_chapter_reward",
        uniqueConstraints = @UniqueConstraint(name = "uk_chapter_reward", columnNames = {"player_id","stage","chapter_label"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainFbChapterRewardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(name = "stage", nullable = false)
    private Integer stage; // chương index từ config

    @Column(name = "chapter_label", nullable = false, length = 32)
    private String chapterLabel; // ví dụ "1-10"

    @Column(name = "claimed")
    private Boolean claimed;

    @Column(name = "claim_ts")
    private Long claimTs;

    public void markClaimed() {
        this.claimed = true;
        this.claimTs = Instant.now().getEpochSecond();
    }
}