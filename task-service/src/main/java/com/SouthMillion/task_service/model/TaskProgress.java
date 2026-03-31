package com.SouthMillion.task_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String taskCode;    // Ví dụ: "BUY_ITEM", "EQUIP_X"
    private int targetId;       // id vật phẩm, id equip,...
    private int progress;       // Tiến độ đã làm
    private int targetAmount;   // Số lượng cần để hoàn thành (config)
    private boolean finished;
}