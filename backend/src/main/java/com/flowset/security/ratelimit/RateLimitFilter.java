package com.flowset.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowset.config.RateLimitProperties;
import com.flowset.exceptions.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory token bucket. Suitable for a single application instance. Not shared across replicas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || limitFor(request) <= 0;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int limit = limitFor(request);
        String key = clientKey(request) + "|" + bucketName(request);
        Instant now = Instant.now();
        prune(now);
        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt.isBefore(now)) {
                return new Window(1, now.plus(Duration.ofMinutes(1)));
            }
            existing.count++;
            return existing;
        });
        if (window != null && window.count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiErrorResponse.of(
                            HttpStatus.TOO_MANY_REQUESTS.value(),
                            "Too Many Requests",
                            "Too many requests. Try again shortly.",
                            request.getRequestURI()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private int limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method)
                && (path.startsWith("/api/v1/auth/login")
                        || path.startsWith("/api/v1/auth/register")
                        || path.startsWith("/api/v1/auth/forgot-password")
                        || path.startsWith("/api/v1/auth/reset-password")
                        || path.startsWith("/api/v1/auth/verify-email")
                        || path.startsWith("/api/v1/auth/resend-verification"))) {
            return properties.authPerMinute();
        }
        if ("POST".equals(method) && path.equals("/api/v1/support/tickets")) {
            return properties.supportPerMinute();
        }
        if ("GET".equals(method) && path.startsWith("/api/v1/search")) {
            return properties.searchPerMinute();
        }
        if (path.startsWith("/api/v1/admin/") && !"GET".equals(method)) {
            return properties.adminPerMinute();
        }
        return 0;
    }

    private static String bucketName(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/")) {
            return "auth";
        }
        if (path.startsWith("/api/v1/search")) {
            return "search";
        }
        if (path.startsWith("/api/v1/support/tickets")) {
            return "support";
        }
        if (path.startsWith("/api/v1/admin/")) {
            return "admin";
        }
        return "other";
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void prune(Instant now) {
        if (windows.size() < 10_000) {
            return;
        }
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt.isBefore(now)) {
                iterator.remove();
            }
        }
    }

    private static final class Window {
        private int count;
        private final Instant expiresAt;

        private Window(int count, Instant expiresAt) {
            this.count = count;
            this.expiresAt = expiresAt;
        }
    }
}
