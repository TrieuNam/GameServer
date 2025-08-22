package com.southMillion.equip_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name = "equip_fumo",
        indexes = {
                @Index(name="idx_role_type", columnList = "role_id,equip_type", unique = true)
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipFumoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", nullable=false, length=64)
    private String roleId;

    @Column(name="equip_type", nullable=false)
    private int equipType;

    @Column(name="level", nullable=false)
    private int level;

    @Column(name="exp", nullable=false)
    private int exp;

    /** epoch giây kết thúc hiệu lực (0 nếu không có) */
    @Column(name="end_time", nullable=false)
    private long endTime;

    @Version
    private long version;

    @Column(name="updated_at", nullable=false)
    private long updatedAt;

    @PrePersist @PreUpdate
    public void touch() {
        updatedAt = Instant.now().getEpochSecond();
    }
}