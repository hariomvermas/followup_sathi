package com.followupsaathi.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory sliding-window limiter for failed login attempts, keyed by
 * email+IP. Good enough for a single-node deployment; a restart simply
 * clears the counters.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> attempts = new HashMap<>();

    /** True when another attempt is still allowed for this key. */
    public synchronized boolean tryAcquire(String key) {
        purge(key, Instant.now());
        Deque<Instant> deque = attempts.get(key);
        return deque == null || deque.size() < MAX_ATTEMPTS;
    }

    /** Records a failed attempt for this key. */
    public synchronized void recordFailure(String key) {
        attempts.computeIfAbsent(key, k -> new ArrayDeque<>())
                .addLast(Instant.now());
    }

    /** Clears the key after a successful login. */
    public synchronized void reset(String key) {
        attempts.remove(key);
    }

    public synchronized int remainingAttempts(String key) {
        purge(key, Instant.now());
        Deque<Instant> deque = attempts.get(key);
        return deque == null ? MAX_ATTEMPTS : MAX_ATTEMPTS - deque.size();
    }

    private void purge(String key, Instant now) {
        Deque<Instant> deque = attempts.get(key);
        if (deque == null) {
            return;
        }
        while (!deque.isEmpty() && deque.peekFirst().plus(WINDOW).isBefore(now)) {
            deque.pollFirst();
        }
        if (deque.isEmpty()) {
            attempts.remove(key);
        }
    }
}
