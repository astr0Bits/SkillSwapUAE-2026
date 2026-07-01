package security;

import config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_PATH = "/api/stripe/webhook";

    private final RateLimitConfig rateLimitConfig;

    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Pass through static resources and the Stripe webhook (verified by signature, not rate-limited)
        if (!path.startsWith("/api/") || path.equals(WEBHOOK_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = resolveBucket(path, ip);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket resolveBucket(String path, String ip) {
        if (path.startsWith("/api/auth/") || path.equals("/api/verify-otp") || path.equals("/api/request-otp")) {
            return rateLimitConfig.resolveAuthBucket(ip);
        }
        if (isPaymentCheckoutPath(path)) {
            return rateLimitConfig.resolvePaymentBucket(ip);
        }
        return rateLimitConfig.resolveGlobalBucket(ip);
    }

    // Matches /api/sponsors/programs/{id}/checkout
    private boolean isPaymentCheckoutPath(String path) {
        return path.startsWith("/api/sponsors/programs/") && path.endsWith("/checkout");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
