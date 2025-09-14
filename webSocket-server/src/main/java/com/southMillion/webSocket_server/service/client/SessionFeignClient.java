package com.southMillion.webSocket_server.service.client;


import feign.FeignException;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.ws.rs.core.HttpHeaders;
import org.SouthMillion.dto.session.LoginDTOs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client gọi vào session-service.
 * - /api/session/login
 * - /api/session/refresh
 * - /api/session/introspect (yêu cầu header Authorization: Bearer <token>)
 */
@FeignClient(
        name = "session-service",
        configuration = SessionFeignClient.FeignCfg.class
)
public interface SessionFeignClient {

    @PostMapping("/api/session/login")
    LoginDTOs.TokenPair login(@RequestBody LoginDTOs.LoginReq req);

    @PostMapping("/api/session/refresh")
    LoginDTOs.TokenPair refresh(@RequestBody LoginDTOs.RefreshReq req);

    /** Gửi trực tiếp header Authorization (Bearer ...) */
    @PostMapping("/internal/session/introspect")
    LoginDTOs.IntrospectResp introspectRaw(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );

    /** Tiện ích: truyền token thô, client tự thêm "Bearer " */
    default LoginDTOs.IntrospectResp introspect(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        return introspectRaw("Bearer " + rawToken);
    }

    // ========= Optional config cho Feign =========
    @Configuration
    class FeignCfg {
        /** Thêm User-Agent để dễ trace log; có thể bỏ nếu không cần */
        @Bean
        public RequestInterceptor userAgentInterceptor() {
            return tpl -> tpl.header(HttpHeaders.USER_AGENT, "websocket-service");
        }

        /** Ném lỗi có nghĩa hơn khi 401/403; để handler xử lý graceful */
        @Bean
        public ErrorDecoder sessionErrorDecoder() {
            ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
            return (methodKey, response) -> {
                int status = response.status();
                if (status == 401 || status == 403) {
                    // Trả về FeignException tương ứng (giữ nguyên status & body)
                    return FeignException.errorStatus(methodKey, response);
                }
                return defaultDecoder.decode(methodKey, response);
            };
        }
    }
}