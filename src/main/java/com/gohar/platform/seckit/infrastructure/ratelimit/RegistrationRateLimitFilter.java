package com.gohar.platform.seckit.infrastructure.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter enforcing in-service rate limiting on user registration submissions (POST /register).
 * Limits requests to 5 attempts per client IP per minute, returning HTTP 429 Too Many Requests when exceeded.
 */
@Component
public class RegistrationRateLimitFilter extends OncePerRequestFilter {

    private final int maxAttempts;
    private final long windowSeconds;
    private final Clock clock;
    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    public RegistrationRateLimitFilter() {
        this(5, 60, Clock.systemUTC());
    }

    public RegistrationRateLimitFilter(int maxAttempts, long windowSeconds, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only throttle POST submissions to /register
        return !( "POST".equalsIgnoreCase(request.getMethod()) && "/register".equals(request.getRequestURI()) );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(windowSeconds);

        Deque<Instant> timestamps = requestLog.computeIfAbsent(clientIp, k -> new ArrayDeque<>());

        boolean allowed;
        synchronized (timestamps) {
            // Evict expired timestamps
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxAttempts) {
                timestamps.addLast(now);
                allowed = true;
            } else {
                allowed = false;
            }
        }

        if (allowed) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowSeconds));

            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE)) {
                response.setContentType("application/problem+json");
                response.setCharacterEncoding("UTF-8");
                String problemJson = String.format(
                    "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Maximum registration attempts exceeded. Please try again in %d seconds.\"}",
                    windowSeconds
                );
                response.getWriter().write(problemJson);
            } else {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                    "<!DOCTYPE html><html><head><title>Too Many Requests</title></head><body><h1>429 Too Many Requests</h1><p>Maximum registration attempts exceeded. Please try again later.</p></body></html>"
                );
            }
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }

    public void clear() {
        requestLog.clear();
    }
}
