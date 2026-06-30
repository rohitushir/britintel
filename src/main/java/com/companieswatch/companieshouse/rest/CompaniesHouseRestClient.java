package com.companieswatch.companieshouse.rest;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Thin client over the Companies House Public Data (REST) API. Used to look up a company's
 * current state on demand (backfill when a user adds it) — NOT for change polling, which is the
 * streaming worker's job (architecture.md).
 *
 * <p>Every call passes through {@link RestRateLimiter} first to stay under the rate limit, and
 * retries 429 responses with exponential back-off as a safety net if the limit is still hit.
 */
@Component
public class CompaniesHouseRestClient {

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final WebClient webClient;
    private final RestRateLimiter rateLimiter;

    public CompaniesHouseRestClient(WebClient companiesHouseWebClient, RestRateLimiter rateLimiter) {
        this.webClient = companiesHouseWebClient;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Fetch a company's current profile.
     *
     * @throws CompanyNotFoundException if Companies House has no such company (404)
     * @throws CompaniesHouseException  on any other failure
     */
    public CompanyProfile fetchProfile(String companyNumber) {
        rateLimiter.acquire();
        try {
            JsonNode body = webClient.get()
                    .uri("/company/{companyNumber}", companyNumber)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND,
                            response -> Mono.error(new CompanyNotFoundException(companyNumber)))
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                            .filter(CompaniesHouseRestClient::isTooManyRequests))
                    .block(REQUEST_TIMEOUT);

            if (body == null) {
                throw new CompaniesHouseException("Empty profile response for " + companyNumber);
            }
            return CompanyProfile.from(companyNumber, body, body.toString());
        } catch (CompaniesHouseException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw new CompaniesHouseException(
                    "Companies House returned " + e.getStatusCode() + " for " + companyNumber, e);
        } catch (RuntimeException e) {
            throw new CompaniesHouseException(
                    "Failed to fetch profile for " + companyNumber, e);
        }
    }

    private static boolean isTooManyRequests(Throwable t) {
        return t instanceof WebClientResponseException e
                && e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
    }
}
