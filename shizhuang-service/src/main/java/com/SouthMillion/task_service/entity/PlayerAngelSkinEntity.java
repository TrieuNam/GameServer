package com.SouthMillion.task_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_angel_skin",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "skin_seq"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAngelSkinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    // Mã thứ tự skin (angel_skin_seq trong config)
    @Column(name = "skin_seq", nullable = false)
    private int skinSeq;

    // Cấp của skin (skin_level)
    @Column(name = "skin_level", nullable = false)
    private int skinLevel;

    // Đã kích hoạt hay chưa
    @Column(name = "activated", nullable = false)
    private boolean activated;

    // Nếu muốn mở rộng: thời gian kích hoạt, hiệu ứng skin riêng, v.v.
}