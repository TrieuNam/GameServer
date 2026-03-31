package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.enity.User;
import com.SouthMillion.user_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.SouthMillion.dto.user.ActiveResp;
import org.SouthMillion.dto.user.VerifyReq;
import org.SouthMillion.dto.user.VerifyResp;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalAuthController {

    private final AuthService auth;

    /**
     * session-service gọi để verify mật khẩu.
     * Request: { username: "account|username", password: "raw" }
     * Response: { ok, userId, username, account }
     */
    @PostMapping(value = "/auth/verify-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifyResp> verifyPassword(@Valid @RequestBody VerifyReq req,
                                                     HttpServletRequest http) {
        User u = auth.verifyPassword(req.getUsername(), req.getPassword());
        if (u == null) {
            return ResponseEntity.ok(new VerifyResp(false, null, null, null));
        }
        return ResponseEntity.ok(new VerifyResp(true, u.getUserId(), u.getUsername(), u.getAccount()));
    }

    /**
     * websocket-server/gateway hỏi trạng thái user có ACTIVE không.
     */
    @GetMapping(value = "/users/{userId}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActiveResp active(@PathVariable("userId") String userId) {
        return new ActiveResp(auth.isActive(userId));
    }
}