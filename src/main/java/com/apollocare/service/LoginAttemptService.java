package com.apollocare.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.apollocare.exception.TooManyRequestsException;

/** Blocks password guessing: 5 failed logins per email within 15 minutes. In-memory (resets on restart). */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        Deque<Instant> q = failures.get(email);
        if (q == null) {
            return;
        }
        synchronized (q) {
            prune(q);
            if (q.size() >= MAX_FAILURES) {
                throw new TooManyRequestsException("Too many failed login attempts. Please try again in 15 minutes.");
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Instant> q = failures.computeIfAbsent(email, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(Instant.now());
        }
    }

    public void reset(String email) {
        failures.remove(email);
    }

    private void prune(Deque<Instant> q) {
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) {
            q.pollFirst();
        }
    }
}
