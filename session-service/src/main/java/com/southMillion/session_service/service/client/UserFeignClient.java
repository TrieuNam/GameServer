package com.southMillion.session_service.service.client;

import org.SouthMillion.dto.user.ActiveResp;
import org.SouthMillion.dto.user.VerifyReq;
import org.SouthMillion.dto.user.VerifyResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Giả định user-service có endpoint nội bộ để verify:
 * POST /internal/user/verify { username, password } -> { ok: true, userId: "...", username: "..." }
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    @PostMapping(
            value = "/internal/auth/verify-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    VerifyResp verifyPassword(@RequestBody VerifyReq req);

    @GetMapping(value = "/internal/{userId}/active",
            produces = MediaType.APPLICATION_JSON_VALUE)
    ActiveResp isActive(@PathVariable("userId") String userId);
}