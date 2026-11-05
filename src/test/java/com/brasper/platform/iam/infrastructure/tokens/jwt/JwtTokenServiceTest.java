package com.brasper.platform.iam.infrastructure.tokens.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789-0123456789-0123456789";

    private static JwtTokenService service(String secret, Duration expiration, boolean allowRandom) {
        return new JwtTokenService(new JwtProperties(secret, expiration, allowRandom));
    }

    private static User savedUser(long id) {
        User user = new User(new SignUpCommand(new Username("ana"), "raw", "Ana", Role.ROLE_ENGINEER), "$2a$hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void validTokenReturnsTheUserId() {
        JwtTokenService jwt = service(SECRET, Duration.ofHours(1), false);

        String token = jwt.generateToken(savedUser(42));

        assertThat(jwt.validateToken(token)).contains(42L);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenService jwt = service(SECRET, Duration.ofSeconds(-1), false);

        assertThat(jwt.validateToken(jwt.generateToken(savedUser(1)))).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = service(SECRET, Duration.ofHours(1), false).generateToken(savedUser(1));

        assertThat(service("another-secret-another-secret-123456", Duration.ofHours(1), false).validateToken(token))
                .isEmpty();
    }

    @Test
    void tamperedAndGarbageTokensAreRejected() {
        JwtTokenService jwt = service(SECRET, Duration.ofHours(1), false);
        String token = jwt.generateToken(savedUser(1));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwt.validateToken(tampered)).isEmpty();
        assertThat(jwt.validateToken("not-a-jwt")).isEmpty();
        assertThat(jwt.validateToken("")).isEmpty();
        assertThat(jwt.validateToken(null)).isEmpty();
    }

    @Test
    void rejectsShortSecrets() {
        assertThatThrownBy(() -> service("too-short", Duration.ofHours(1), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankSecretIsOnlyAllowedWhenRandomKeysAreAllowed() {
        assertThatThrownBy(() -> service("", Duration.ofHours(1), false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service(null, Duration.ofHours(1), false)).isInstanceOf(IllegalStateException.class);

        JwtTokenService dev = service("", Duration.ofHours(1), true);
        assertThat(dev.validateToken(dev.generateToken(savedUser(5)))).contains(5L);
    }

    @Test
    void unsavedUsersGetNoToken() {
        User unsaved = new User(new SignUpCommand(new Username("ana"), "raw", "Ana", Role.ROLE_ADMIN), "$2a$hash");

        assertThatThrownBy(() -> service(SECRET, Duration.ofHours(1), false).generateToken(unsaved))
                .isInstanceOf(NullPointerException.class);
    }
}
