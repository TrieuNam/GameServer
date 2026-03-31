package com.SouthMillion.worlds_ervice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "world_bosses", indexes = {
    @Index(name = "idx_boss_status", columnList = "status"),
    @Index(name = "idx_spawn_time", columnList = "spawnTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldBoss {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bossId;
    
    @Column(nullable = false, length = 100)
    private String bossName;
    
    @Column(nullable = false)
    private Integer bossLevel;
    
    @Column(nullable = false)
    @Builder.Default
    private Long currentHp = 0L;
    
    @Column(nullable = false)
    @Builder.Default
    private Long maxHp = 1000000L;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BossStatus status = BossStatus.NOT_SPAWNED;
    
    @Column(length = 100)
    private String location; // Map/zone location
    
    private Instant spawnTime;
    
    private Instant defeatedTime;
    
    @Column(length = 50)
    private String defeatedBy; // Player or guild that dealt final blow
    
    @Column(columnDefinition = "JSON")
    private String rewards; // JSON rewards config
    
    @CreationTimestamp
    private Instant createTime;
    
    @UpdateTimestamp
    private Instant updateTime;
    
    public enum BossStatus {
        NOT_SPAWNED,
        SPAWNING,
        ACTIVE,
        DEFEATED,
        DESPAWNED
    }
    
    public Integer getHpPercentage() {
        if (maxHp == 0) return 0;
        return (int) ((currentHp * 100) / maxHp);
    }
}
