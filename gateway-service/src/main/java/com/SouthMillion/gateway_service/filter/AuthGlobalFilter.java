package com.SouthMillion.gateway_service.filter;


import com.SouthMillion.gateway_service.config.AppAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AppAuthProperties props;
    private final WebClient webClient;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public int getOrder() {
        // Sau CORS/preflight, trước routing
        return -20;
    }
    // ===== helpers =====

    private boolean isWhitelisted(String path) {
        var wl = props.getWhitelist();
        if (wl == null || wl.isEmpty()) return false;
        for (String p : wl) if (matcher.match(p, path)) return true;
        return false;
    }

    private String tokenFromHttp(HttpHeaders headers, URI uri) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        // fallback query (?token=)
        String qk = props.getWs().getQueryTokenKey();
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst(qk);
    }

    private String tokenFromWs(HttpHeaders headers, URI uri) {
        // 1) query ?token=
        String t = UriComponentsBuilder.fromUri(uri).build()
                .getQueryParams()
                .getFirst(props.getWs().getQueryTokenKey());
        if (StringUtils.hasText(t)) return t;

        // 2) iterate header keys
        for (String hk : props.getWs().getHeaderTokenKeys()) {
            List<String> values = headers.getOrEmpty(hk);
            if (values == null || values.isEmpty()) continue;

            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(hk)) {
                for (String v : values) {
                    if (StringUtils.hasText(v) && v.startsWith("Bearer ")) {
                        return v.substring(7).trim();
                    }
                }
                continue;
            }

            if ("Sec-WebSocket-Protocol".equalsIgnoreCase(hk)) {
                for (String v : values) {
                    if (!StringUtils.hasText(v)) continue;
                    for (String part : v.split(",")) {
                        String candidate = part.trim();
                        if (candidate.startsWith("token.")) {
                            candidate = candidate.substring("token.".length());
                        }
                        if (candidate.startsWith("Bearer ")) {
                            candidate = candidate.substring(7).trim();
                        }
                        if (isLikelyJwt(candidate)) {
                            return candidate;
                        }
                    }
                }
                continue;
            }

            for (String v : values) {
                if (StringUtils.hasText(v)) return v.trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String,Object>> introspect(String token) {
        String serviceId = props.getSessionServiceId();
        String path      = props.getIntrospectPath();
        String method    = props.getIntrospectMethod();

        WebClient.RequestHeadersSpec<?> spec;
        if ("GET".equalsIgnoreCase(method)) {
            spec = webClient.get()
                    .uri(b -> b.scheme("lb").host(serviceId).path(path)
                            .queryParam("token", token).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON);
        } else {
            spec = webClient.post()
                    .uri(b -> b.scheme("lb").host(serviceId).path(path).build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.TEXT_PLAIN)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(token);
        }

        ParameterizedTypeReference<Map<String,Object>> MAP = new ParameterizedTypeReference<>() {};

        return spec.exchangeToMono(resp -> {
            int sc = resp.statusCode().value();

            if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(MAP)
                        .defaultIfEmpty(Map.of()) // body rỗng vẫn OK
                        .map(body -> {
                            Map<String,Object> m = new java.util.HashMap<>(body);
                            // Nếu service không trả cờ, mặc định active=true khi 2xx
                            m.putIfAbsent("active", true);
                            return m;
                        });
            }

            if (sc == 401 || sc == 403 || sc == 404) {
                return resp.bodyToMono(String.class).defaultIfEmpty("")
                        .map(body -> Map.<String,Object>of("active", false, "reason", "http-" + sc));
            }

            // 5xx và các lỗi khác -> ném exception để onErrorResume xử lý
            return resp.createException().flatMap(Mono::error);
        });
    }

    private Mono<Void> unauthorized(ServerWebExchange ex, String reason) {
        var resp = ex.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("X-Auth-Reason", reason);
        return resp.setComplete();
    }

    private static String asStr(Object o) { return o == null ? null : String.valueOf(o); }

    private static boolean isLikelyJwt(String s) {
        if (!StringUtils.hasText(s)) return false;
        int dots = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '.') dots++;
        return dots == 2;
    }

    private static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        if (v instanceof String s) {
            String t = s.trim();
            return "true".equalsIgnoreCase(t) || "1".equals(t) ||
                    "yes".equalsIgnoreCase(t) || "y".equalsIgnoreCase(t) ||
                    "ok".equalsIgnoreCase(t) || "valid".equalsIgnoreCase(t);
        }
        return false;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var req = exchange.getRequest();
        var path = req.getURI().getPath();

        // Bỏ qua preflight
        if (req.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Whitelist
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        boolean isWebSocket = "websocket".equalsIgnoreCase(req.getHeaders().getUpgrade());

        String token = isWebSocket
                ? tokenFromWs(req.getHeaders(), req.getURI())
                : tokenFromHttp(req.getHeaders(), req.getURI());

        // Debug nhanh
        // log.info("[AUTH] path={} upgrade={} token.len={}", path, req.getHeaders().getUpgrade(), token==null?0:token.length());

        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "missing-token");
        }

        return introspect(token)
                .flatMap(info -> {
                    boolean active = truthy(info.get("active"))
                            || truthy(info.get("ok"))
                            || truthy(info.get("valid"));
                    if (!active) {
                        String reason = asStr(info.get("reason"));
                        return unauthorized(exchange, reason != null ? reason : "invalid-token");
                    }
                    ServerWebExchange mutated = exchange.mutate().request(builder -> {
                        String userId = asStr(info.get("userId"));
                        String sessionId = asStr(info.get("sessionId"));
                        if (StringUtils.hasText(userId)) {
                            builder.header("X-User-Id", userId);
                            builder.header("X-Route-Key", userId);
                        }
                        if (StringUtils.hasText(sessionId)) {
                            builder.header("X-Session-Id", sessionId);
                        }
                    }).build();
                    return chain.filter(mutated);
                })
                .onErrorResume(ex -> unauthorized(exchange, "auth-error"));
    }
}