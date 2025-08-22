package com.southMillion.session_service.config;

import com.southMillion.session_service.service.RateLimitService;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService svc;
    private final RateLimitProperties props;

    private String ip(HttpServletRequest req){
        String h = req.getHeader("X-Forwarded-For");
        if (h!=null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        final long nowSec = System.currentTimeMillis() / 1000;

        String path = req.getRequestURI();
        String key;
        int limit;

        if (path.startsWith("/api/session/login")) {
            // Nếu login gửi JSON body, không lấy được username ở đây.
            // Đơn giản hoá: rate limit theo IP.
            key = "rl:login:" + ip(req);
            limit = props.getLoginLimit();

            // Nếu muốn theo username, hãy truyền thêm header X-Login-User từ client/gateway rồi:
            // String user = Optional.ofNullable(req.getHeader("X-Login-User")).orElse("");
            // key = "rl:login:" + ip(req) + ":" + user;

        } else if (path.startsWith("/api/session/refresh")) {
            key = "rl:refresh:" + ip(req);
            limit = props.getRefreshLimit();
        } else {
            key = "rl:generic:" + ip(req);
            limit = props.getGenericLimit();
        }

        var d = svc.allow(key, limit, props.getWindowSeconds());

        // Headers: Limit, Remaining, Reset (epoch seconds)
        resp.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        resp.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - d.count())));
        resp.setHeader("X-RateLimit-Reset", String.valueOf(d.resetAtEpochSec()));

        if (!d.allowed()) {
            long retry = Math.max(1, d.resetAtEpochSec() - nowSec);  // giây còn lại
            resp.setHeader("Retry-After", String.valueOf(retry));
            resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }
}