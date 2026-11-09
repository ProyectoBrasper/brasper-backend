package com.brasper.platform.iam.infrastructure.tokens.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code secret} must be at least 32 bytes. A blank secret is only accepted when
 * {@code allowRandomSecret} is true (set by the dev profile), so prod can never start with a throwaway key.
 */
@ConfigurationProperties(prefix = "brasper.security.jwt")
public record JwtProperties(String secret, @DefaultValue("8h") Duration expiration, boolean allowRandomSecret) {
}
