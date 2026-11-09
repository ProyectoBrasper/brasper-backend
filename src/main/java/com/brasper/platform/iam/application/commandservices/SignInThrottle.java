package com.brasper.platform.iam.application.commandservices;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Stops password guessing: after {@value #MAX_FAILURES} wrong passwords in a row a username is locked for
 * {@value #LOCK_MINUTES} minutes, whoever asks. A successful sign-in clears the count.
 * shortcut: counted in memory per username, so a restart clears it and several instances would not share it; the
 * deployment is a single instance. A locked username cannot sign in even with the right password until the lock ends.
 */
@Component
public class SignInThrottle {

    static final int MAX_FAILURES = 5;
    static final int LOCK_MINUTES = 15;
    private static final int MAX_TRACKED_USERNAMES = 10_000;

    private record Attempts(int failures, Instant lockedUntil) { }

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public SignInThrottle() {
        this(Clock.systemUTC());
    }

    SignInThrottle(Clock clock) {
        this.clock = clock;
    }

    public boolean isLocked(String username) {
        Attempts found = attempts.get(key(username));
        return found != null && found.lockedUntil() != null && clock.instant().isBefore(found.lockedUntil());
    }

    public void failed(String username) {
        if (attempts.size() >= MAX_TRACKED_USERNAMES) {
            attempts.clear(); // random usernames cannot grow this without bound
        }
        Instant now = clock.instant();
        attempts.compute(key(username), (name, found) -> {
            // a lock that already ended starts a new count
            int failures = found == null || (found.lockedUntil() != null && !now.isBefore(found.lockedUntil()))
                    ? 1 : found.failures() + 1;
            return new Attempts(failures, failures >= MAX_FAILURES ? now.plus(Duration.ofMinutes(LOCK_MINUTES)) : null);
        });
    }

    public void succeeded(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }
}
