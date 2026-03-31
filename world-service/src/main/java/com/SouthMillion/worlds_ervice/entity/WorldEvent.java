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
@Table(name = "world_events", indexes = {
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_start_time", columnList = "startTime"),
    @Index(name = "idx_end_time", columnList = "endTime"),
    @Index(name = "idx_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;
    
    @Column(nullable = false, length = 100)
    private String eventName;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "JSON")
    private String eventData; // JSON config for event
    
    @Column(nullable = false)
    private Instant startTime;
    
    @Column(nullable = false)
    private Instant endTime;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean recurring = false;
    
    @Column(length = 50)
    private String cronSchedule; // For recurring events
    
    @CreationTimestamp
    private Instant createTime;
    
    @UpdateTimestamp
    private Instant updateTime;
    
    public enum EventType {
        WORLD_BOSS,      // World boss spawn
        DOUBLE_EXP,      // Double experience event
        DOUBLE_DROP,     // Double drop rate event
        SPECIAL_SHOP,    // Limited time shop
        SIEGE_WAR,       // Guild siege war
        FESTIVAL,        // Seasonal festival
        MAINTENANCE,     // Scheduled maintenance
        CUSTOM           // Custom event
    }
}
