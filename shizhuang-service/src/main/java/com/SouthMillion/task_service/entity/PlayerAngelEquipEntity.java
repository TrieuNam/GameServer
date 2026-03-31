package com.SouthMillion.task_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Angel entity disabled in shizhuang-service (belongs to angel-service)
// @Entity
// @Table(name = "player_angel_equip", uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "position"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAngelEquipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    // Vị trí trang bị (0~N: vũ khí, áo, nhẫn, vòng...)
    @Column(name = "position", nullable = false)
    private int position;

    // Mã trang bị hiện tại (equipment_id trong config)
    @Column(name = "equipment_id", nullable = false)
    private int equipmentId;

    // Level trang bị (nếu có hệ thống nâng cấp)
    @Column(name = "level", nullable = false)
    private int level;

    // Có thể mở rộng: thuộc tính cường hoá, thời gian trang bị, trạng thái khoá...
}