package com.brasper.platform.iam.infrastructure.pipeline;

import com.brasper.platform.iam.application.outboundservices.TokenService;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from {@code Authorization: Bearer <jwt>}. The user is reloaded on every request so a
 * deactivated user or a changed role takes effect immediately. Without a valid token the request stays anonymous.
 */
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenService tokens;
    private final UserRepository users;

    public BearerAuthorizationRequestFilter(TokenService tokens, UserRepository users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            tokens.validateToken(header.substring(PREFIX.length()).strip())
                    .flatMap(users::findById)
                    .filter(user -> user.isActive())
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(
                                    user.getUsername().value(),
                                    null,
                                    List.of(new SimpleGrantedAuthority(user.getRole().name())))));
        }
        chain.doFilter(request, response);
    }
}
