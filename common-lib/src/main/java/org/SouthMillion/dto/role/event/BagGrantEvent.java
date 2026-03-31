package org.SouthMillion.dto.role.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BagGrantEvent {
    private String eventId;
    private String userId;
    private String roleId;
    private String source;     // "first-login", "quest", ...
    private Instant createdAt;
    private List<Item> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private Integer itemId;
        private Integer num;
        private Boolean bind;
        private Instant expireAt; // có thể null
    }
}