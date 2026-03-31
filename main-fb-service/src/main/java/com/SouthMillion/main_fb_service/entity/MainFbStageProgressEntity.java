package com.SouthMillion.main_fb_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "main_fb_stage_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_progress_player_stage_level", columnNames = {"player_id","stage","level"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainFbStageProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(name = "stage", nullable = false)
    private Integer stage;  // chương

    @Column(name = "level", nullable = false)
    private Integer level;  // ải trong chương

    @Column(name = "best_score")
    private Integer bestScore;  // tuỳ game: điểm/"sao"

    @Column(name = "clear_count")
    private Integer clearCount; // số lần vượt

    @Column(name = "first_clear_ts")
    private Long firstClearTs;  // epoch seconds

    @Column(name = "last_clear_ts")
    private Long lastClearTs;   // epoch seconds

    public void onClear(int score) {
        long now = Instant.now().getEpochSecond();
        this.clearCount = (this.clearCount == null ? 0 : this.clearCount) + 1;
        this.lastClearTs = now;
        if (this.firstClearTs == null) this.firstClearTs = now;
        if (this.bestScore == null || score > this.bestScore) this.bestScore = score;
    }
}