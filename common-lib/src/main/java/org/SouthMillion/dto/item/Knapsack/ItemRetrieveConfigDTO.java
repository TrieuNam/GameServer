package org.SouthMillion.dto.item.Knapsack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration DTO for item retrieve (回收) rules
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRetrieveConfigDTO {

    @JsonProperty("retrieve_list")
    private List<RetrieveEntry> retrieveList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieveEntry {
        @JsonProperty("item_id")
        private Integer itemId;

        @JsonProperty("retrieve_item_id")
        private Integer retrieveItemId;

        @JsonProperty("retrieve_num")
        private Integer retrieveNum;
    }
}
