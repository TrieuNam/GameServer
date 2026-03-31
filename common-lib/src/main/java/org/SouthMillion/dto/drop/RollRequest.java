package org.SouthMillion.dto.drop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RollRequest {
    @NotNull String roleId;       // đồng bộ naming với bag-service
    @NotNull Integer dropId;
    @Min(1) int times = 1;

    Options options = new Options();

    @Data
    public static class Options {
        boolean noRepeat = false;
        String pityGroup;        // nếu null -> "drop:{dropId}"
        Long forceSeed;          // test/tái hiện
        boolean applyToBag = false; // true => gọi internal/bag/add (cần bật app.bag.applyEnabled)
        Integer bagType;         // nếu cần mapping loại túi
    }
}