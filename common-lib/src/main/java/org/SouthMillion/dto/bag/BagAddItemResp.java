package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("success")
    private Boolean succeeded;

    private List<BagAddItemReq.Item> added;
    private String message;
    private Integer errorCode;

    // Instance method to check if successful
    public boolean isSuccess() {
        return Boolean.TRUE.equals(succeeded);
    }

    // Alias for backward compatibility  
    public boolean isOk() {
        return Boolean.TRUE.equals(succeeded);
    }

    // Accessor for error message
    public String error() {
        return message != null ? message : "Unknown error";
    }

    // Static factory methods
    public static BagAddItemResp ok(List<BagAddItemReq.Item> added) {
        return new BagAddItemResp(true, added, null, null);
    }

    public static BagAddItemResp fail(String message) {
        return new BagAddItemResp(false, null, message, -1);
    }

    public static BagAddItemResp fail(String message, Integer errorCode) {
        return new BagAddItemResp(false, null, message, errorCode);
    }

    // Alias for JSON compatibility
    public Boolean success() {
        return succeeded;
    }

    public void setSuccess(Boolean success) {
        this.succeeded = success;
    }

    public List<BagAddItemReq.Item> added() {
        return added;
    }
}

