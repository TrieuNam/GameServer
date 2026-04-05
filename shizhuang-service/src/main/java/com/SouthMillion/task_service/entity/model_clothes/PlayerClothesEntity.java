package com.SouthMillion.task_service.entity.model_clothes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_clothes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerClothesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "clothes_id")
    private Integer clothesId;

    @Column(name = "level")
    private Integer level;

    @Builder.Default
    @Column(name = "wearing", nullable = false)
    private Boolean wearing = false;
}
