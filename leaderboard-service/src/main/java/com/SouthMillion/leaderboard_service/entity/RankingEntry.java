package com.SouthMillion.leaderboard_service.entity;

import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Ranking Entry Entity
 * 
 * Ranking Types:
 * 1 = Power
 * 2 = Level
 * 3 = Arena
 * 4 = Wealth
 * 5 = Guild
 * 6 = Pet
 * 7 = Mount
 * 8 = PVP Kills
 */
@Entity
@Table(name = "ranking_entry", indexes = {
    @Index(name = "idx_rank_type", columnList = "rankingType"),
    @Index(name = "idx_rank_score", columnList = "rankingType,score"),
    @Index(name = "idx_rank_role", columnList = "roleId")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rankingType", "roleId"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer rankingType; // 1=Power, 2=Level, 3=Arena, etc.

    @Column(nullable = false, length = 50)
    private String roleId;

    @Column(nullable = false, length = 50)
    private String roleName;

    @Column(nullable = false)
    private Integer roleLevel;

    @Column(nullable = false)
    @Builder.Default
    private Long score = 0L;

    @Column
    private Integer currentRank;

    @Column
    private Integer previousRank;

    @Column(length = 50)
    private String guildName;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Integer getRankChange() {
        if (previousRank == null || currentRank == null) {
            return 0;
        }
        return previousRank - currentRank; // Positive = rank up, Negative = rank down
    }

    public void updateRank(Integer newRank) {
        this.previousRank = this.currentRank;
        this.currentRank = newRank;
    }
}
