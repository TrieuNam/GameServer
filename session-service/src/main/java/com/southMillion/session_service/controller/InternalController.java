package com.southMillion.session_service.controller;

import com.southMillion.session_service.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.IntrospectResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/session")
public class InternalController {
    private final JwtService jwt;

    // Cho gateway kiểm tra nhanh access token
    @PostMapping("/introspect")
    public ResponseEntity<IntrospectResponse> introspect(@RequestBody String accessToken) {
        try {
            Jws<Claims> j = jwt.parse(accessToken);
            Claims c = j.getBody();
            boolean isAccess = "access".equals(c.get("type"));
            if (!isAccess) return ResponseEntity.ok(IntrospectResponse.builder().active(false).build());
            return ResponseEntity.ok(IntrospectResponse.builder()
                    .active(true)
                    .userId(c.getSubject())
                    .username(String.valueOf(c.get("uname")))
                    .sessionId(String.valueOf(c.get("sid")))
                    .exp(c.getExpiration().toInstant().getEpochSecond())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(IntrospectResponse.builder().active(false).build());
        }
    }
}