package com.brasper.platform.iam.infrastructure.pipeline;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Writes one line per request that changes something, saying who did it: {@code ana POST /api/v1/invoices/7/void ->
 * 200}. Only the user, the method, the path and the status are logged, never the body (it can hold passwords).
 * shortcut: the trail is the application log, so it lasts as long as the host keeps logs; move it to a table if
 * accounting needs it for years.
 */
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger audit = LoggerFactory.getLogger("audit");
    private static final Set<String> READ_ONLY = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            if (!READ_ONLY.contains(request.getMethod())) {
                Authentication user = SecurityContextHolder.getContext().getAuthentication();
                audit.info("{} {} {} -> {}", user == null ? "anonymous" : user.getName(), request.getMethod(),
                        request.getRequestURI(), response.getStatus());
            }
        }
    }
}
