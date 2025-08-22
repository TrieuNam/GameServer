package org.SouthMillion.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class H5VerifyRequest {
    @NotBlank
    private String spid;
    @NotBlank
    private String device;
    @NotBlank
    private String userId;     // từ H5
    @NotBlank
    private String timestamp;  // chuỗi epoch seconds
    @NotBlank
    private String sign;       // HMAC-SHA256(spid|device|userId|timestamp, secret[spid])
}