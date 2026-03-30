package com.SouthMillion.equip_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "equip_slot",
        indexes = {
                @Index(name="idx_role", columnList = "role_id"),
                @Index(name="idx_role_type", columnList = "role_id,equip_type", unique = true)
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipSlotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="role_id", nullable=false)
    private Long roleId;

    /** Vị trí trang bị (helmet/chest/weapon…) — map từ meta item */
    @Column(name="equip_type", nullable=false)
    private int equipType;

    /** ItemId đang mặc ở slot này (0 nếu trống) */
    @Column(name="item_id", nullable=false)
    private int itemId;

    // optional: snapshot stat khi mặc (tuỳ hệ thống)
    @Column(name="hp")
    private int hp;
    @Column(name="attack")
    private int attack;
    @Column(name="defend")
    private int defend;
    @Column(name="speed")
    private int speed;
    @Column(name="attr_type1")
    private int attrType1;
    @Column(name="attr_value1")
    private int attrValue1;
    @Column(name="attr_type2")
    private int attrType2;
    @Column(name="attr_value2")
    private int attrValue2;

    @Version
    private long version;

    @Column(name="updated_at", nullable=false)
    private long updatedAt;

    @PrePersist @PreUpdate
    public void touch() {
        updatedAt = Instant.now().getEpochSecond();
    }
}