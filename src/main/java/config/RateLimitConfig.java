package config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // One bucket map for general/global traffic, keyed by IP
    private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();

    // One bucket map for sensitive auth endpoints, keyed by IP
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    // One bucket map for payment/checkout endpoints, keyed by IP
    private final Map<String, Bucket> paymentBuckets = new ConcurrentHashMap<>();

    /**
     * Returns (or creates) a general-purpose rate limit bucket for the given IP.
     * Allows 50 requests per minute per IP.
     */
    public Bucket resolveGlobalBucket(String ipAddress) {
        return globalBuckets.computeIfAbsent(ipAddress, ip -> {
            Bandwidth limit = Bandwidth.classic(50, Refill.greedy(50, Duration.ofMinutes(1)));
            return Bucket4j.builder().addLimit(limit).build();
        });
    }

    /**
     * Returns (or creates) an auth-specific rate limit bucket for the given IP.
     * Allows 5 login/reset attempts per hour per IP.
     */
    public Bucket resolveAuthBucket(String ipAddress) {
        return authBuckets.computeIfAbsent(ipAddress, ip -> {
            Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));
            return Bucket4j.builder().addLimit(limit).build();
        });
    }

    /**
     * Returns (or creates) a payment-specific rate limit bucket for the given IP.
     * Allows 10 checkout attempts per hour per IP.
     */
    public Bucket resolvePaymentBucket(String ipAddress) {
        return paymentBuckets.computeIfAbsent(ipAddress, ip -> {
            Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
            return Bucket4j.builder().addLimit(limit).build();
        });
    }
}