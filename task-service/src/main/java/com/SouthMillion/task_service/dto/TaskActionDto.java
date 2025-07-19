package com.SouthMillion.task_service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskActionDto {
    private String type;               // Ví dụ: "COMPLETE_TASK", "CLAIM_REWARD"
    private Long taskId;               // ID nhiệm vụ
    private Map<String, Object> params; // Tham số bổ sung cho từng loại action (nếu cần)
}
