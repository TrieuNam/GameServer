package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.enity.UserAccount;
import com.SouthMillion.user_service.repository.UserAccountRepository;
import com.SouthMillion.user_service.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.RegisterRequest;
import org.SouthMillion.dto.user.RegisterResponse;
import org.SouthMillion.dto.user.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserAccountService svc;
    private final UserAccountRepository repo;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(@RequestParam String username) {
        return ResponseEntity.ok(repo.existsByUsername(username));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserAccount u = svc.register(req);
        return ResponseEntity.ok(new RegisterResponse(u.getId(), u.getUsername()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> get(@PathVariable String userId) {
        UserAccount u = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        return ResponseEntity.ok(new UserProfile(
                u.getId(), u.getUsername(), u.getStatus(), u.getSpid(), u.getExternalId(), u.getDevice(),
                u.getCreatedAt().getEpochSecond(),
                u.getLastLoginAt() == null ? null : u.getLastLoginAt().getEpochSecond()
        ));
    }
}