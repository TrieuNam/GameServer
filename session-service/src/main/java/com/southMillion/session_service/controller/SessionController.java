package com.southMillion.session_service.controller;

import com.southMillion.session_service.service.SessionService;
import com.southMillion.session_service.service.SessionStore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.LogoutRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session")
public class SessionController {
    private final SessionService sessionService;
    private final SessionStore store;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req) {
        sessionService.logout(req.getSessionId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list/{userId}")
    public ResponseEntity<Set<String>> list(@PathVariable String userId){
        return ResponseEntity.ok(store.listByUser(userId));
    }

    @PostMapping("/revoke-all/{userId}")
    public ResponseEntity<Map<String,String>> revokeAll(@PathVariable String userId){
        store.revokeAll(userId);
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}