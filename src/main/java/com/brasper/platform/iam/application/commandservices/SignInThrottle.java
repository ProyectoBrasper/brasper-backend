package com.brasper.platform.iam.application.commandservices;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Stops password guessing: after {@value #MAX_FAILURES} wrong passwords in a row from one source (the client IP) for
 * one username, that pair is locked for {@value #LOCK_MINUTES} minutes. A successful sign-in clears the count.
 * Counting per source means someone who only knows a username cannot lock the real user out from another address.
 * shortcut: counted in memory, so a restart clears it and several instances would not share it; the deployment is a
 * single instance. Someone with many addresses gets {@value #MAX_FAILURES} guesses per address; bcrypt and the
 * password rules are the limit there. Behind a proxy the source is the proxy address unless the server reads
 * X-Forwarded-For (see {@code server.forward-headers-strategy} in application-prod.yml); then all users share one
 * count, as before.
 */
@Component
public class SignInThrottle {

    static final int MAX_FAILURES = 5;
    static final int LOCK_MINUTES = 15;
    private static final int MAX_TRACKED = 10_000;

    private record Attempts(int failures, Instant lockedUntil) { }

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public SignInThrottle() {
        this(Clock.systemUTC());
    }

    SignInThrottle(Clock clock) {
        this.clock = clock;
    }

    public boolean isLocked(String username, String source) {
        Attempts found = attempts.get(key(username, source));
        return found != null && found.lockedUntil() != null && clock.instant().isBefore(found.lockedUntil());
    }

    public void failed(String username, String source) {
        if (attempts.size() >= MAX_TRACKED) {
            attempts.clear(); // random usernames cannot grow this without bound
        }
        Instant now = clock.instant();
        attempts.compute(key(username, source), (name, found) -> {
            // a lock that already ended starts a new count
            int failures = found == null || (found.lockedUntil() != null && !now.isBefore(found.lockedUntil()))
                    ? 1 : found.failures() + 1;
            return new Attempts(failures, failures >= MAX_FAILURES ? now.plus(Duration.ofMinutes(LOCK_MINUTES)) : null);
        });
    }

    public void succeeded(String username, String source) {
        attempts.remove(key(username, source));
    }

    /** Lifts every lock and count of a username, from any source (an admin reset the password). */
    public void clear(String username) {
        String prefix = key(username, "");
        attempts.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static String key(String username, String source) {
        return (username == null ? "" : username.toLowerCase(Locale.ROOT)) + "|" + (source == null ? "" : source);
    }
}
