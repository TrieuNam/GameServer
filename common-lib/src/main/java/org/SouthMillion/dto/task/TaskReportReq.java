package org.SouthMillion.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskReportReq {
    private String playerId;
    private String taskKey;
    private Integer progressDelta;  // Số lượng tiến độ tăng thêm
}
