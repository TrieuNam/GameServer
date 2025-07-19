package com.SouthMillion.battleserver_service.event;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BattleEvent {
    private String battleId;
    private String eventType; // e.g. START, END
    private String data; // Bạn có thể cho thêm field cụ thể
    private long timestamp;
}