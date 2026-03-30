package com.SouthMillion.block_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "role_block_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleBlockState {

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "activate_seq", nullable = false)
    private Integer activateSeq;

    @Column(name = "wearing_map_id", nullable = false)
    private Integer wearingMapId;

    @Column(name = "next_block_index", nullable = false)
    private Integer nextBlockIndex;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
