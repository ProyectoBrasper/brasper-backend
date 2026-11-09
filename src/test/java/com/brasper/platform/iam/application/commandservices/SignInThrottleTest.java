package com.brasper.platform.iam.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SignInThrottleTest {

    private Instant now = Instant.parse("2026-11-08T10:00:00Z");
    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    };
    private final SignInThrottle throttle = new SignInThrottle(clock);

    private void fail(String username, int times) {
        for (int i = 0; i < times; i++) {
            throttle.failed(username);
        }
    }

    @Test
    void locksAUsernameAfterFiveFailuresRegardlessOfCaseAndUnlocksAfterFifteenMinutes() {
        fail("Ana", 4);
        assertThat(throttle.isLocked("ana")).isFalse();

        throttle.failed("ANA");
        assertThat(throttle.isLocked("ana")).isTrue();
        assertThat(throttle.isLocked("luis")).isFalse();

        now = now.plus(Duration.ofMinutes(14));
        assertThat(throttle.isLocked("ana")).isTrue();
        now = now.plus(Duration.ofMinutes(2));
        assertThat(throttle.isLocked("ana")).isFalse();
    }

    @Test
    void aSuccessfulSignInClearsTheCountAndAnEndedLockStartsANewOne() {
        fail("ana", 4);
        throttle.succeeded("ana");
        fail("ana", 4);
        assertThat(throttle.isLocked("ana")).isFalse();

        throttle.failed("ana");
        now = now.plus(Duration.ofMinutes(16));
        throttle.failed("ana");
        assertThat(throttle.isLocked("ana")).isFalse();
    }
}
