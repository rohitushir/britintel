package com.companieswatch.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the Clerk Backend API, used at first sign-in to look up a user's email address
 * (Clerk's default session token does not carry it). Authenticated with the Clerk secret key.
 */
@Component
public class ClerkBackendClient {

    private final RestClient restClient;

    public ClerkBackendClient(@Value("${clerk.api-base-url:https://api.clerk.com/v1}") String apiBaseUrl,
                              @Value("${clerk.secret-key:}") String secretKey) {
        this.restClient = RestClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .build();
    }

    /** Primary email for the Clerk user, or the first on file. */
    public String fetchPrimaryEmail(String clerkUserId) {
        ClerkUser user = restClient.get()
                .uri("/users/{id}", clerkUserId)
                .retrieve()
                .body(ClerkUser.class);
        if (user == null || user.emailAddresses() == null || user.emailAddresses().isEmpty()) {
            throw new IllegalStateException("Clerk user " + clerkUserId + " has no email address");
        }
        return user.emailAddresses().stream()
                .filter(e -> e.id() != null && e.id().equals(user.primaryEmailAddressId()))
                .map(ClerkUser.EmailAddress::emailAddress)
                .findFirst()
                .orElseGet(() -> user.emailAddresses().get(0).emailAddress());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ClerkUser(
            @JsonProperty("primary_email_address_id") String primaryEmailAddressId,
            @JsonProperty("email_addresses") List<EmailAddress> emailAddresses) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record EmailAddress(String id, @JsonProperty("email_address") String emailAddress) {
        }
    }
}
