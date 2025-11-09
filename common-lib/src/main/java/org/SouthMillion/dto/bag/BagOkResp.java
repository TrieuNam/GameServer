package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Generic OK response (standalone class for direct import)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BagOkResp {
    private Boolean success;
    private String message;
    private Integer errorCode;

    public static BagOkResp ok() {
        return new BagOkResp(true, "Success", null);
    }

    public static BagOkResp error(String message) {
        return new BagOkResp(false, message, -1);
    }

    public Boolean success() {
        return success;
    }

    public String message() {
        return message;
    }
}

