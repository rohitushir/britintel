package com.companieswatch.companieshouse.streaming;

import com.companieswatch.config.CompaniesHouseProperties;
import com.companieswatch.config.WebClientConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Real {@link StreamSource} backed by the JDK {@link HttpClient}. Uses a line-oriented body
 * handler ({@code ofLines}) which fits the newline-delimited streaming protocol and a blocking
 * worker thread, avoiding reactive backpressure plumbing for a single long-lived connection.
 */
@Component
public class HttpStreamSource implements StreamSource, AutoCloseable {

    private final CompaniesHouseProperties properties;
    private final HttpClient httpClient;

    public HttpStreamSource(CompaniesHouseProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public Stream<String> open(CompaniesHouseStream stream, Long fromTimepoint) throws Exception {
        var config = properties.getStreaming();
        String uri = config.getBaseUrl() + stream.path()
                + (fromTimepoint != null ? "?timepoint=" + fromTimepoint : "");

        // No request timeout: this connection is meant to stay open indefinitely.
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .header(HttpHeaders.AUTHORIZATION, WebClientConfig.basicAuth(config.getApiKey()))
                .GET()
                .build();

        HttpResponse<Stream<String>> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

        int status = response.statusCode();
        if (status == 200) {
            return response.body();
        }
        // Drain/close the error body, then signal the right handling.
        try (var body = response.body()) {
            body.close();
        } catch (RuntimeException ignored) {
            // best-effort
        }
        if (status == 416) {
            throw new StaleTimepointException("Timepoint " + fromTimepoint + " too old for " + stream);
        }
        throw new IOException("Stream " + stream.configName() + " returned HTTP " + status);
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
