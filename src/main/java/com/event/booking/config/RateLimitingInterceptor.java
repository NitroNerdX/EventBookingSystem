package com.event.booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter per user/IP.
 * - Authenticated users: rate-limited by username
 * - Anonymous users: rate-limited by remote IP
 *
 * Note: uses a simple fixed window counter. For production consider
 * a distributed store (Redis) or a library like Bucket4j with proper eviction.
 */
@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 100; // per window
    private static final long WINDOW_MS = 60_000L; // 1 minute

    private static record RateLimit(long windowStart, int count) {}

    private final Map<String, RateLimit> limits = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String key = clientKey(request);
        long now = System.currentTimeMillis();

        RateLimit rl = limits.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart() >= WINDOW_MS) {
                return new RateLimit(now, 1);
            }
            return new RateLimit(old.windowStart(), old.count() + 1);
        });

        if (rl.count() > MAX_REQUESTS) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MS - (now - rl.windowStart())) / 1000);
            log.warn("Rate limit exceeded for key={} count={}", key, rl.count());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write("Too Many Requests");
            return false;
        }

        log.debug("Rate limit ok for key={} count={}", key, rl.count());
        return true;
    }

    private String clientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "user:" + auth.getName();
        }
        String ip = request.getRemoteAddr();
        return "ip:" + ip;
    }
}
