package com.companieswatch.companieshouse.rest;

import com.companieswatch.config.CompaniesHouseProperties;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Blocking token-bucket throttle around the REST client. Keeps us under the Companies House
 * limit (~2 req/sec; ~600 per 5-min window) so we proactively avoid 429s rather than
 * discovering the limit at runtime (data-sources.md).
 *
 * <p>Refill is lazy (computed on each acquire from elapsed time) so there is no background
 * thread. A single bucket is shared across all callers, which is correct for v1's single
 * instance; if the app is ever scaled out, throttling must move server-side or to a shared store.
 */
@Component
public class RestRateLimiter {

    private final double permitsPerSecond;
    private final double maxBurst;
    private final ReentrantLock lock = new ReentrantLock();

    private double availablePermits;
    private long lastRefillNanos;

    public RestRateLimiter(CompaniesHouseProperties props) {
        this.permitsPerSecond = props.getRest().getPermitsPerSecond();
        this.maxBurst = Math.max(1, props.getRest().getBurst());
        this.availablePermits = this.maxBurst;
        this.lastRefillNanos = System.nanoTime();
    }

    /** Block until one permit is available, then consume it. */
    public void acquire() {
        try {
            while (true) {
                long sleepMillis;
                lock.lock();
                try {
                    refill();
                    if (availablePermits >= 1.0) {
                        availablePermits -= 1.0;
                        return;
                    }
                    double deficit = 1.0 - availablePermits;
                    sleepMillis = (long) Math.ceil(deficit / permitsPerSecond * 1000.0);
                } finally {
                    lock.unlock();
                }
                Thread.sleep(Math.max(1, sleepMillis));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a rate-limit permit", e);
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSeconds > 0) {
            availablePermits = Math.min(maxBurst, availablePermits + elapsedSeconds * permitsPerSecond);
            lastRefillNanos = now;
        }
    }
}
