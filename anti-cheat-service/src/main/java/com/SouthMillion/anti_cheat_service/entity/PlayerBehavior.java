package com.SouthMillion.anti_cheat_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_behaviors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBehavior {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
    
    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // MOVEMENT_SPEED, DAMAGE_DEALT, RESOURCE_GAINED, ACTION_RATE
    
    @Column(name = "metric_value", nullable = false)
    private Double metricValue;
    
    @Column(name = "expected_value")
    private Double expectedValue;
    
    @Column(name = "deviation")
    private Double deviation; // Standard deviations from expected
    
    @Column(name = "is_anomaly", nullable = false)
    private Boolean isAnomaly;
    
    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData; // JSON: position, level, equipment, etc.
    
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
