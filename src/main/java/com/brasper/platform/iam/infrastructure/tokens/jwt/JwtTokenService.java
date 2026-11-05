package com.brasper.platform.iam.infrastructure.tokens.jwt;

import com.brasper.platform.iam.application.outboundservices.TokenService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** HS256 tokens. The key must be at least 256 bits. */
@Service
public class JwtTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final int MIN_KEY_BYTES = 32;

    private final byte[] key;
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            if (!properties.allowRandomSecret()) {
                throw new IllegalStateException("JWT_SECRET is required (min. 32 bytes)");
            }
            key = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(key);
            log.warn("brasper.security.jwt.secret is not set: using a random key, tokens are lost on restart");
        } else {
            key = secret.getBytes(StandardCharsets.UTF_8);
            if (key.length < MIN_KEY_BYTES) {
                throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
            }
        }
    }

    @Override
    public String generateToken(User user) {
        Objects.requireNonNull(user.getId(), "Only saved users get tokens");
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername().value())
                .claim("role", user.getRole().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(properties.expiration())))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign token", e);
        }
    }

    @Override
    public Optional<Long> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(key))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiresAt = claims.getExpirationTime();
            if (expiresAt == null || !expiresAt.after(new Date())) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (ParseException | JOSEException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
