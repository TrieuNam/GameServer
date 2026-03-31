package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * New Server Competition (新服比拼) for RandActivity type 29.
 * Competition activity for new servers with milestone rewards.
 */
@Entity
@Table(name = "new_server_competition")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NewServerCompetition {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false, unique = true)
    private Long roleId;

    /** JSON array of fetch flags for different competition categories */
    @Column(name = "fetch_flag_json", nullable = false, columnDefinition = "TEXT")
    private String fetchFlagJson;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
