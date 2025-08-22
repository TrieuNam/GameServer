package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.enity.UserAccount;
import com.SouthMillion.user_service.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.H5VerifyRequest;
import org.SouthMillion.dto.user.VerifyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Các API nội bộ giữa services.
 */
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserAccountService svc;

    /**
     * Khớp với session-service:
     * POST /internal/user/verify { username, password } -> { ok, userId, username }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String,Object>> verify(@Valid @RequestBody VerifyRequest req) {
        var u = svc.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!svc.verifyPassword(u, req.getPassword())) {
            throw new IllegalArgumentException("invalid password");
        }
        svc.markLogin(u.getId());
        return ResponseEntity.ok(svc.toVerifyResp(u));
    }

    /**
     * Dành cho H5 verify theo flow client:
     * body: { spid, device, userId, timestamp, sign }
     * -> nếu hợp lệ: auto tạo user (username = spid_userId) nếu chưa tồn tại.
     */
    @PostMapping("/h5-verify")
    public ResponseEntity<Map<String,Object>> h5Verify(@Valid @RequestBody H5VerifyRequest req) {
        UserAccount u = svc.verifyH5AndProvision(req);
        return ResponseEntity.ok(svc.toVerifyResp(u));
    }
}