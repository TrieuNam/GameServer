package com.SouthMillion.pet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_mount")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id")
    private String playerId;

    @Column(name = "mount_id")
    private Integer mountId;

    @Column(name = "level")
    private Integer level;

    @Column(name = "exp")
    private Integer exp;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "skin_id")
    private Integer skinId;

    @Column(name = "created_time")
    private Long createdTime;
}