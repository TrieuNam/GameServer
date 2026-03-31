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
@Table(name = "world_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorldState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String stateKey; // e.g., "GLOBAL_STATE", "SERVER_1_STATE"
    
    @Column(columnDefinition = "JSON")
    private String stateData; // JSON object containing state information
    
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;
    
    @CreationTimestamp
    private Instant createTime;
    
    @UpdateTimestamp
    private Instant updateTime;
}
