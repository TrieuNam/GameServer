package com.SouthMillion.task_service.filter;

import com.SouthMillion.task_service.client.AuthVerifyFeignClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
public class ExternalJwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private AuthVerifyFeignClient authVerifyFeignClient;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            try {
                Map<String, Object> result = authVerifyFeignClient.verifyToken(Map.of("token", token));
                Boolean valid = (Boolean) result.get("valid");
                if (Boolean.TRUE.equals(valid)) {
                    String username = (String) result.get("username");
                    User principal = new User(username, "", Collections.emptyList());
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ex) {
                // token không hợp lệ hoặc user-service lỗi
            }
        }
        filterChain.doFilter(request, response);
    }
}