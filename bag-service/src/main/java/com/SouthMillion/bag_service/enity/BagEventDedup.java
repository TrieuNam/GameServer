package com.SouthMillion.bag_service.enity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bag_event_dedup")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BagEventDedup {
    @Id
    @Column(name = "event_id", length = 100, nullable = false)
    private String eventId;

    // DB-managed timestamp cho log/cleanup
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "timestamp(3) not null default current_timestamp(3)")
    private Instant createdAt;
}