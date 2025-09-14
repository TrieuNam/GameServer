package com.SouthMillion.config_service.core;

import com.SouthMillion.config_service.config.InternalProps;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "app.internal", name = "enabled", havingValue = "true")
public class InternalTokenFilter extends OncePerRequestFilter {
    private final InternalProps props;

    public InternalTokenFilter(InternalProps props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        return path == null || !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String required = props.token();
        String provided = req.getHeader("X-Internal-Token");
        if (required != null && !required.isBlank() && required.equals(provided)) chain.doFilter(req, res);
        else res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid X-Internal-Token");
    }
}