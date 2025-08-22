package com.southMillion.session_service.controller;

import com.southMillion.session_service.service.SessionService;
import com.southMillion.session_service.service.client.UserFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.session.H5LoginResponse;
import org.SouthMillion.dto.session.LoginRequest;
import org.SouthMillion.dto.session.RefreshRequest;
import org.SouthMillion.dto.session.TokenPair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session")
public class AuthController {
    private final UserFeignClient userClient;
    private final SessionService sessionService;

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        Map<String,Object> result = userClient.verify(Map.of(
                "username", req.getUsername(),
                "password", req.getPassword()
        ));
        boolean ok = Boolean.TRUE.equals(result.get("ok"));
        if (!ok) return ResponseEntity.status(401).build();
        String userId = String.valueOf(result.get("userId"));
        String username = String.valueOf(result.getOrDefault("username", req.getUsername()));
        String ip = ip(http); String ua = http.getHeader("User-Agent");
        TokenPair tokens = sessionService.issue(userId, username, ip, ua);
        return ResponseEntity.ok(tokens);
    }

    // ==== NEW: Login H5 GET tương thích Client (verify_url)
    // GET /api/session/login?spid=...&device=...&userId=...&timestamp=...&sign=...
    @CrossOrigin(origins = "*")
    @GetMapping("/login")
    public ResponseEntity<H5LoginResponse> h5Login(
            @RequestParam(name = "spid") String spid,
            @RequestParam(name = "device") String device,
            @RequestParam(name = "userId") String userId,
            @RequestParam(name = "timestamp") long timestamp,
            @RequestParam(name = "sign", required = false, defaultValue = "") String sign,
            HttpServletRequest http
    ) {
        long now = Instant.now().getEpochSecond();
        // Chống replay ở mức cơ bản (tùy chỉnh thêm nếu cần)
        if (Math.abs(now - timestamp) > 600) { // lệch quá 10 phút
            return ResponseEntity.ok(H5LoginResponse.error(1001, "timestamp_skew"));
        }

        // TODO: Nếu có secret per-spid: verify 'sign' tại đây (MD5/HMAC tùy kênh).
        // Với 'dev' có thể bỏ qua check:
        // if (!"dev".equalsIgnoreCase(spid)) { ... verify ... }

        String ip = ip(http);
        String ua = http.getHeader("User-Agent");
        // Dùng userId làm username mặc định (client không gửi username)
        TokenPair tokens = sessionService.issue(userId, userId, ip, ua);

        H5LoginResponse.User u = new H5LoginResponse.User();
        u.setAccount(userId);
        u.setAccount_type(0);
        u.setFcm_flag(0);
        u.setLogin_sign(tokens.getAccessToken()); // access JWT để tái sử dụng
        u.setLogin_time(now);
        u.setUid(userId);
        u.setOpenid(userId);
        u.setMerger_spid(spid);

        H5LoginResponse resp = new H5LoginResponse();
        resp.setRet(0);
        resp.setUser(u);
        resp.setRole_data(Collections.emptyMap()); // có thể điền nếu đã có dữ liệu nhân vật
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody RefreshRequest req) {
        TokenPair tokens = sessionService.refresh(req.getRefreshToken());
        return ResponseEntity.ok(tokens);
    }

    private String ip(HttpServletRequest req){
        String h = req.getHeader("X-Forwarded-For");
        if (h!=null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}