package com.SouthMillion.bag_service.enity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "bag_meta",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_bag", columnNames = {"role_id", "bag_type"})
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BagMeta {

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id; // roleId + "-" + bagType

    @Column(name = "role_id", nullable = false, length = 64) // <— đồng bộ với SQL
    private String roleId;

    @Column(name = "bag_type", nullable = false)
    private byte bagType;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "used", nullable = false)
    private int used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = idOf(roleId, bagType);
        var now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static String idOf(String roleId, int bagType) {
        return roleId + "-" + bagType;
    }
}