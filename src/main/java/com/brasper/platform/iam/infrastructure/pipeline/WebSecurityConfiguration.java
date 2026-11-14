package com.brasper.platform.iam.infrastructure.pipeline;

import com.brasper.platform.iam.application.outboundservices.TokenService;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless JWT security. Roles are enforced per endpoint with {@code @PreAuthorize("hasRole('ADMIN')")}. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    /** The frontend lives on another domain (Vercel), so the browser needs these origins allowed. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${brasper.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        // The browser hides these from the frontend unless they are exposed: the page total and the new resource URL.
        cors.setExposedHeaders(List.of("X-Total-Count", "Location"));
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokens, UserRepository users)
            throws Exception {
        return http
                // Uses the CorsConfigurationSource bean; answers the preflight before the token check.
                .cors(Customizer.withDefaults())
                // No cookies or sessions: every request carries its own bearer token, so CSRF does not apply.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/auth/sign-in").permitAll()
                        // API docs describe the endpoints, they expose no data; each call still needs a token.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                // Anonymous requests get 401 (Spring's default would be 403).
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(new BearerAuthorizationRequestFilter(tokens, users),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new AuditLogFilter(), BearerAuthorizationRequestFilter.class)
                .build();
    }
}
