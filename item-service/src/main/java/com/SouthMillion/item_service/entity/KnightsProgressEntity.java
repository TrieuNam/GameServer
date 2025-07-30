package com.SouthMillion.item_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "knights_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnightsProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id") // mapping đúng cột user_id trong DB
    private String userId;

    @Column(name = "level")
    private int level;

    @Column(name = "flag")
    private int flag; // bitmask nhận thưởng theo seq

    @Column(name = "level_flag")
    private int levelFlag; // bitmask nhận thưởng theo level
}