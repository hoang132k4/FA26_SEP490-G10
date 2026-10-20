package com.jlpp.modules.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for password reset requests
 * to prevent email bombing, brute force, and abuse.
 */
@Component
public class RateLimiter {

    @Value("${app.reset-password.rate-limit-per-window:3}")
    private int maxRequests;

    @Value("${app.reset-password.rate-limit-window-minutes:15}")
    private int windowMinutes;

    private final Map<String, List<Instant>> requestLog = new ConcurrentHashMap<>();

    public synchronized boolean isAllowed(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        Instant now = Instant.now();
        Instant windowStart = now.minus(windowMinutes, ChronoUnit.MINUTES);

        List<Instant> timestamps = requestLog.computeIfAbsent(key.toLowerCase(), k -> new ArrayList<>());

        // Remove timestamps outside current window
        timestamps.removeIf(t -> t.isBefore(windowStart));

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.add(now);
        return true;
    }

    public synchronized void reset(String key) {
        if (key != null) {
            requestLog.remove(key.toLowerCase());
        }
    }

    public synchronized void clear() {
        requestLog.clear();
    }
}
