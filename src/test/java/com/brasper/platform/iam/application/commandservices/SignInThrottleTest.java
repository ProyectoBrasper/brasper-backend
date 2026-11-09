package com.brasper.platform.iam.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SignInThrottleTest {

    private static final String HOME = "10.0.0.1";
    private static final String ATTACKER = "203.0.113.9";

    private Instant now = Instant.parse("2026-11-08T10:00:00Z");
    private final Clock clock = new Clock() {
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    };
    private final SignInThrottle throttle = new SignInThrottle(clock);

    private void fail(String username, String source, int times) {
        for (int i = 0; i < times; i++) {
            throttle.failed(username, source);
        }
    }

    @Test
    void locksAUsernameFromOneSourceAfterFiveFailuresRegardlessOfCaseAndUnlocksAfterFifteenMinutes() {
        fail("Ana", HOME, 4);
        assertThat(throttle.isLocked("ana", HOME)).isFalse();

        throttle.failed("ANA", HOME);
        assertThat(throttle.isLocked("ana", HOME)).isTrue();
        assertThat(throttle.isLocked("luis", HOME)).isFalse();

        now = now.plus(Duration.ofMinutes(14));
        assertThat(throttle.isLocked("ana", HOME)).isTrue();
        now = now.plus(Duration.ofMinutes(2));
        assertThat(throttle.isLocked("ana", HOME)).isFalse();
    }

    @Test
    void anotherSourceCannotLockTheRealUserOut() {
        fail("admin", ATTACKER, 5);

        assertThat(throttle.isLocked("admin", ATTACKER)).isTrue();
        assertThat(throttle.isLocked("admin", HOME)).isFalse();
    }

    @Test
    void aSuccessfulSignInClearsTheCountAndAnEndedLockStartsANewOne() {
        fail("ana", HOME, 4);
        throttle.succeeded("ana", HOME);
        fail("ana", HOME, 4);
        assertThat(throttle.isLocked("ana", HOME)).isFalse();

        throttle.failed("ana", HOME);
        now = now.plus(Duration.ofMinutes(16));
        throttle.failed("ana", HOME);
        assertThat(throttle.isLocked("ana", HOME)).isFalse();
    }

    @Test
    void clearLiftsTheLocksOfAUsernameFromEverySourceAndLeavesOthersAlone() {
        fail("ana", HOME, 5);
        fail("ana", ATTACKER, 5);
        fail("luis", HOME, 5);

        throttle.clear("Ana");

        assertThat(throttle.isLocked("ana", HOME)).isFalse();
        assertThat(throttle.isLocked("ana", ATTACKER)).isFalse();
        assertThat(throttle.isLocked("luis", HOME)).isTrue();
    }
}
