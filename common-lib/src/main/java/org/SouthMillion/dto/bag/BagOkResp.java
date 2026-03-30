package org.SouthMillion.dto.bag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("success")
    private Boolean succeeded;

    private String message;
    private Integer errorCode;

    // Static factory methods
    public static BagOkResp ok() {
        return new BagOkResp(true, "Success", null);
    }

    public static BagOkResp fail() {
        return new BagOkResp(false, "Error", -1);
    }

    public static BagOkResp fail(String message) {
        return new BagOkResp(false, message, -1);
    }

    public static BagOkResp fail(String message, Integer errorCode) {
        return new BagOkResp(false, message, errorCode);
    }

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

    // Alias for JSON compatibility
    public Boolean success() {
        return succeeded;
    }

    public void setSuccess(Boolean success) {
        this.succeeded = success;
    }
}

