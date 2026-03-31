package com.SouthMillion.task_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Angel entity disabled in shizhuang-service (belongs to angel-service)
// @Entity
// @Table(name = "player_angel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAngelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, unique = true)
    private Long playerId;

    // Cấp thiên sứ hiện tại
    @Column(name = "angel_level", nullable = false)
    private int angelLevel;

    // Bậc thiên sứ hiện tại
    @Column(name = "angel_grade", nullable = false)
    private int angelGrade;

    // Mã skin thiên sứ đang dùng
    @Column(name = "used_skin_seq")
    private Integer usedSkinSeq;

    // Có thể mở rộng thêm field nếu logic cần (mốc timestamp, trạng thái tiến hoá, ...)
}