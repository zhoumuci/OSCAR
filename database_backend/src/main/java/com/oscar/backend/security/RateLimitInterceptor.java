package com.oscar.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-tier IP-based rate limiter with abuse ban.
 *
 * <h3>Tier 1 — soft limit</h3>
 * Sliding-window request counter per IP.  When exceeded the client receives
 * HTTP 429 with a short JSON message so legitimate users know to slow down.
 *
 * <h3>Tier 2 — abuse ban</h3>
 * An IP that triggers the soft limit repeatedly within a short abuse-detection
 * window is promoted to a <em>banned</em> state.  Banned IPs receive no response
 * body — only a bare HTTP 429 status and a closed connection.
 * The ban expires after a configurable duration.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitInterceptor.class);

    // ---- soft limit ----
    private final long windowMillis;
    private final int maxRequestsPerWindow;
    private final Map<String, Deque<Long>> windowByIp = new ConcurrentHashMap<>();

    // ---- abuse detection ----
    private final int abuseThreshold;
    private final long abuseWindowMillis;

    // ---- ban ----
    private final long banDurationMillis;
    private final Map<String, Long> bannedUntilByIp = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> violationTimestampsByIp = new ConcurrentHashMap<>();

    public RateLimitInterceptor(
            int windowSeconds,
            int maxRequestsPerWindow,
            int abuseThreshold,
            int abuseWindowSeconds,
            int banDurationSeconds
    ) {
        this.windowMillis = windowSeconds * 1_000L;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.abuseThreshold = abuseThreshold;
        this.abuseWindowMillis = abuseWindowSeconds * 1_000L;
        this.banDurationMillis = banDurationSeconds * 1_000L;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        String clientIp = extractClientIp(request);
        long now = System.currentTimeMillis();

        // ---- tier 2: banned IP — silent reject ----
        Long bannedUntil = bannedUntilByIp.get(clientIp);
        if (bannedUntil != null) {
            if (now < bannedUntil) {
                // Still banned — return bare status, no body.
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getOutputStream().close();
                return false;
            }
            // Ban expired.
            bannedUntilByIp.remove(clientIp, bannedUntil);
            violationTimestampsByIp.remove(clientIp);
            LOGGER.info("Ban expired for IP {}", clientIp);
        }

        // ---- tier 1: sliding-window rate check ----
        Deque<Long> timestamps = windowByIp.computeIfAbsent(clientIp, key -> new ArrayDeque<>());

        synchronized (timestamps) {
            long windowStart = now - windowMillis;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequestsPerWindow) {
                recordViolation(clientIp, now);
                LOGGER.warn("Rate limit exceeded for IP {}: {} requests in {} s",
                        clientIp, timestamps.size() + 1, windowMillis / 1_000L);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\","
                                + "\"message\":\"Rate limit exceeded. Please retry later.\"}"
                );
                return false;
            }

            timestamps.offerLast(now);
        }

        return true;
    }

    /**
     * Record a rate-limit violation and ban the IP if the abuse threshold is
     * crossed within the abuse detection window.
     */
    private void recordViolation(String clientIp, long now) {
        Deque<Long> violations = violationTimestampsByIp.computeIfAbsent(
                clientIp, key -> new ArrayDeque<>());

        synchronized (violations) {
            long windowStart = now - abuseWindowMillis;
            while (!violations.isEmpty() && violations.peekFirst() < windowStart) {
                violations.pollFirst();
            }
            violations.offerLast(now);

            if (violations.size() >= abuseThreshold) {
                long banUntil = now + banDurationMillis;
                bannedUntilByIp.put(clientIp, banUntil);
                violationTimestampsByIp.remove(clientIp);
                windowByIp.remove(clientIp);
                LOGGER.warn(
                        "IP {} banned for {} s after {} rate-limit violations within {} s",
                        clientIp,
                        banDurationMillis / 1_000L,
                        violations.size(),
                        abuseWindowMillis / 1_000L
                );
            }
        }
    }

    /**
     * Derive the client IP, honouring common proxy headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma < 0 ? ip.trim() : ip.substring(0, comma).trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }
}
