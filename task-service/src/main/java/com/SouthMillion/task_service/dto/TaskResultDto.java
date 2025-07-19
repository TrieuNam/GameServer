package com.SouthMillion.task_service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskResultDto {
    private String type;                 // Loại action
    private Long taskId;                 // ID nhiệm vụ
    private boolean success;             // Đã thành công chưa
    private String message;              // Thông điệp trả về
    private Map<String, Object> data;    // Data bổ sung (vd: phần thưởng, trạng thái mới...)
}
