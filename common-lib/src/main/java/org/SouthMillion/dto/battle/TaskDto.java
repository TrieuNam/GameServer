package org.SouthMillion.dto.battle;

import lombok.Data;

@Data
public class TaskDto {
    private Long id;
    private String name;
    private String status;
    private Long userId;
}
