package com.apollocare.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.apollocare.exception.TooManyRequestsException;

/**
 * The chat endpoint is public (the FAQ page needs no login), so it is limited per visitor and overall -
 * otherwise anyone could run up the AI bill. In-memory: resets when the backend restarts.
 */
@Component
public class ChatRateLimiter {

    private final int perVisitor;
    private final Duration window;
    private final int globalPerHour;

    private final Map<String, Deque<Instant>> visitors = new ConcurrentHashMap<>();
    private final Deque<Instant> global = new ArrayDeque<>();

    public ChatRateLimiter(@Value("${app.chat.max-per-window:20}") int perVisitor,
                           @Value("${app.chat.window-minutes:10}") int windowMinutes,
                           @Value("${app.chat.global-per-hour:300}") int globalPerHour) {
        this.perVisitor = perVisitor;
        this.window = Duration.ofMinutes(windowMinutes);
        this.globalPerHour = globalPerHour;
    }

    public void check(String visitorKey) {
        Instant now = Instant.now();

        synchronized (global) {
            prune(global, now.minus(Duration.ofHours(1)));
            if (global.size() >= globalPerHour) {
                throw new TooManyRequestsException("The assistant is very busy right now. Please try again in a little while.");
            }
        }

        if (visitors.size() > 5000) {
            visitors.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
        Deque<Instant> q = visitors.computeIfAbsent(visitorKey, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q, now.minus(window));
            if (q.size() >= perVisitor) {
                throw new TooManyRequestsException("You are asking very quickly. Please wait a few minutes and try again.");
            }
            q.addLast(now);
        }
        synchronized (global) {
            global.addLast(now);
        }
    }

    private static void prune(Deque<Instant> q, Instant cutoff) {
        while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) {
            q.pollFirst();
        }
    }
}
