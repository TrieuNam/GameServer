package org.SouthMillion.dto.drop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data @Builder
public class RollResult {
    Integer dropId;
    List<Item> rolled;
    PityInfo pity;
    ApplyBag apply; // thông tin áp dụng nếu có

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class Item {
        int itemId; int num; int bind; int broadcast;
    }
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class PityInfo {
        boolean applied;
        Integer counterBefore;
        Integer threshold;
    }
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ApplyBag {
        boolean attempted;
        boolean success;
        Map<String, Object> rawResponse;
    }
}