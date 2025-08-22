package com.SouthMillion.pet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_harness")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerHarnessEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id")
    private String playerId;

    @Column(name = "harness_id")
    private Integer harnessId;

    @Column(name = "wearing_mark")
    private Integer wearingMark; // 1: đã trang bị, 0: chưa

    // Các trường thuộc tính khác (có thể random, tẩy luyện, lock flag, ...)
    @Column(name = "attr_type")
    private String attrType; // lưu JSON hoặc dạng chuỗi nếu là list

    @Column(name = "attr_value")
    private String attrValue; // lưu JSON hoặc dạng chuỗi nếu là list

    @Column(name = "lock_flag")
    private Integer lockFlag;

    @Column(name = "created_time")
    private Long createdTime;
}