package com.companieswatch.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient beans for the Companies House APIs.
 *
 * <p>Auth is HTTP Basic with the API key as the username and a blank password
 * (data-sources.md), pre-encoded into a default Authorization header.
 */
@Configuration
@EnableConfigurationProperties(CompaniesHouseProperties.class)
public class WebClientConfig {

    /** WebClient used by the REST client for on-demand lookups / backfill (step 3). */
    @Bean
    public WebClient companiesHouseWebClient(CompaniesHouseProperties props) {
        return WebClient.builder()
                .baseUrl(props.getRest().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(props.getRest().getApiKey()))
                .build();
    }

    /** HTTP Basic header for Companies House: API key as username, blank password. */
    public static String basicAuth(String apiKey) {
        String token = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
