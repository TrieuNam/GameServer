package com.SouthMillion.gateway_service.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    public JwtGlobalFilter() {
        System.out.println(">>> REAL JwtGlobalFilter CONSTRUCTOR");
    }

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println(">>> JWT FILTER RUN, PATH: " + exchange.getRequest().getPath());

        String path = exchange.getRequest().getPath().toString();
        // Bỏ qua JWT check cho các route public, ví dụ login/register
        if (path.startsWith("/user-service/auth") || path.startsWith("/report-service/api/report")
        || path.startsWith("/webSocket-server/ws/game")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());  // Sửa ở đây
            Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}