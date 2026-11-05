package com.brasper.platform.iam.infrastructure.pipeline;

import com.brasper.platform.iam.application.outboundservices.TokenService;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Stateless JWT security. Roles are enforced per endpoint with {@code @PreAuthorize("hasRole('ADMIN')")}. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokens, UserRepository users)
            throws Exception {
        return http
                // No cookies or sessions: every request carries its own bearer token, so CSRF does not apply.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/auth/sign-in").permitAll()
                        .anyRequest().authenticated())
                // Anonymous requests get 401 (Spring's default would be 403).
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new BearerAuthorizationRequestFilter(tokens, users),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
