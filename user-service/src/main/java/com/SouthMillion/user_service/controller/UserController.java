package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.api.ApiError;
import org.SouthMillion.dto.user.RegisterReq;
import org.SouthMillion.dto.user.UserResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterReq req) {
        try {
            var u = users.createUser(req.getAccount(), req.getUsername(), req.getPassword());
            return ResponseEntity.ok(new UserResp(u.getUserId(), u.getAccount(), u.getUsername(), u.getStatus().name()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> get(@PathVariable String userId) {
        return users.findById(userId)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(
                        new UserResp(u.getUserId(), u.getAccount(), u.getUsername(), u.getStatus().name())
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}