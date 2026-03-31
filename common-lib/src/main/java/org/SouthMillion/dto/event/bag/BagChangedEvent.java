package org.SouthMillion.dto.event.bag;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BagChangedEvent {
    private String eventId;   // cùng id với grant, để trace/idempotent
    private String userId;
    private String roleId;
    private Integer itemId;
    private Long delta;       // +N/-N (thông tin thêm)
    private Long newNum;      // tồn kho mới sau commit
    private String reason;    // "grant" | "use" | "sell" | ...
    private Instant at;
}