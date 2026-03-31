package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.api.ApiError;
import org.SouthMillion.dto.user.RegisterReq;
import org.SouthMillion.dto.user.UserResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints accessible without token
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;

    /**
     * Register new user
     * POST /api/auth/register
     * Request: { account, username, password }
     * Response: { userId, account, username, status }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {
        try {
            var u = users.createUser(req.getAccount(), req.getUsername(), req.getPassword());
            return ResponseEntity.ok(new UserResp(u.getUserId(), u.getAccount(), u.getUsername(), u.getStatus().name()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
        }
    }
}
