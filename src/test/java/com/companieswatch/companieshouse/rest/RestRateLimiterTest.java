package com.companieswatch.companieshouse.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.companieswatch.config.CompaniesHouseProperties;
import org.junit.jupiter.api.Test;

class RestRateLimiterTest {

    @Test
    void throttlesBeyondTheBurstAllowance() {
        CompaniesHouseProperties props = new CompaniesHouseProperties();
        props.getRest().setPermitsPerSecond(10.0); // fast so the test stays quick
        props.getRest().setBurst(2);
        RestRateLimiter limiter = new RestRateLimiter(props);

        long start = System.nanoTime();
        // 2 permits are immediately available (burst); the 3rd must wait ~1/10s for a refill.
        limiter.acquire();
        limiter.acquire();
        limiter.acquire();
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(80);
    }
}
