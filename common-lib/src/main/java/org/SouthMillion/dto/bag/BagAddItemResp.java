package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Response for add item operation (standalone class for direct import)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BagAddItemResp {
    private Boolean success;
    private List<BagDTOs.ItemDelta> added;
    private String message;

    public Boolean success() {
        return success;
    }

    public List<BagDTOs.ItemDelta> added() {
        return added;
    }
}

