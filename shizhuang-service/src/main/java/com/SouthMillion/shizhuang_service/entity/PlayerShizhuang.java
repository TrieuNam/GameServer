package com.SouthMillion.shizhuang_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_shizhuang",
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "shizhuang_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerShizhuang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "shizhuang_id", nullable = false)
    private int shizhuangId;

    @Column(nullable = false)
    @Builder.Default
    private int level = 1;

    @Column(nullable = false)
    @Builder.Default
    private int star = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean activated = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean wearing = false;
}
