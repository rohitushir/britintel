package com.companieswatch.companieshouse.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.companieswatch.config.CompaniesHouseProperties;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Exercises profile parsing and 404 handling without a live key, by backing the WebClient with a
 * stub exchange function that returns canned responses.
 */
class CompaniesHouseRestClientTest {

    private final RestRateLimiter rateLimiter =
            new RestRateLimiter(new CompaniesHouseProperties());

    private CompaniesHouseRestClient clientReturning(HttpStatus status, String body) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost")
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(status)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new CompaniesHouseRestClient(webClient, rateLimiter);
    }

    @Test
    void parsesProfileFromOkResponse() {
        String json = """
                {
                  "company_number": "12345678",
                  "company_name": "ACME BORROWER LTD",
                  "company_status": "active",
                  "date_of_creation": "2010-05-04",
                  "registered_office_address": { "postal_code": "EC1A 1BB", "locality": "London" }
                }
                """;
        CompanyProfile profile = clientReturning(HttpStatus.OK, json).fetchProfile("12345678");

        assertThat(profile.companyNumber()).isEqualTo("12345678");
        assertThat(profile.companyName()).isEqualTo("ACME BORROWER LTD");
        assertThat(profile.companyStatus()).isEqualTo("active");
        assertThat(profile.dateOfCreation()).isEqualTo(LocalDate.of(2010, 5, 4));
        assertThat(profile.registeredOfficeJson()).contains("EC1A 1BB");
        assertThat(profile.rawJson()).contains("ACME BORROWER LTD");
    }

    @Test
    void mapsNotFoundToTypedException() {
        assertThatThrownBy(() -> clientReturning(HttpStatus.NOT_FOUND, "{}").fetchProfile("99999999"))
                .isInstanceOf(CompanyNotFoundException.class);
    }
}
