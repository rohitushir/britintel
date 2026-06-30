package com.companieswatch.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed config for the Companies House APIs. Values come from env vars / profiles
 * (see application.yml); secrets are never hardcoded.
 */
@ConfigurationProperties(prefix = "companieshouse")
public class CompaniesHouseProperties {

    private final Rest rest = new Rest();
    private final Streaming streaming = new Streaming();

    public Rest getRest() {
        return rest;
    }

    public Streaming getStreaming() {
        return streaming;
    }

    /** Public Data (REST) API — on-demand lookups / backfill. */
    public static class Rest {
        private String baseUrl = "https://api.company-information.service.gov.uk";
        private String apiKey = "";
        /**
         * Throttle target. The REST limit is ~600 requests / 5 min (~2/sec) across all
         * endpoints; we stay under it from day one (data-sources.md).
         */
        private double permitsPerSecond = 2.0;
        /** Small burst allowance so short bursts of adds don't serialise to exactly 2/sec. */
        private int burst = 4;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public void setPermitsPerSecond(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }
    }

    /** Streaming API — real-time change feed (the core; data-sources.md). */
    public static class Streaming {
        private String baseUrl = "https://stream.companieshouse.gov.uk";
        private String apiKey = "";
        /** Master switch; also auto-skips if no API key is configured. */
        private boolean enabled = true;
        /**
         * Which streams to consume. Each is a separate connection and Companies House caps an
         * account at TWO concurrent connections, so this list may hold at most 2 (enforced).
         * v1 defaults to ONE (company profile: status + address changes). Add "charges" as a
         * second connection to also catch the headline charge events.
         */
        private List<String> streams = new ArrayList<>(List.of("companies"));
        /** Reconnect back-off: first delay and ceiling (exponential between). */
        private long reconnectBaseDelayMillis = 1_000;
        private long reconnectMaxDelayMillis = 60_000;
        /** Persist the resume timepoint every N processed events (and on disconnect). */
        private int flushEvery = 25;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getStreams() {
            return streams;
        }

        public void setStreams(List<String> streams) {
            this.streams = streams;
        }

        public long getReconnectBaseDelayMillis() {
            return reconnectBaseDelayMillis;
        }

        public void setReconnectBaseDelayMillis(long reconnectBaseDelayMillis) {
            this.reconnectBaseDelayMillis = reconnectBaseDelayMillis;
        }

        public long getReconnectMaxDelayMillis() {
            return reconnectMaxDelayMillis;
        }

        public void setReconnectMaxDelayMillis(long reconnectMaxDelayMillis) {
            this.reconnectMaxDelayMillis = reconnectMaxDelayMillis;
        }

        public int getFlushEvery() {
            return flushEvery;
        }

        public void setFlushEvery(int flushEvery) {
            this.flushEvery = flushEvery;
        }
    }
}
