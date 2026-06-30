package com.companieswatch.companieshouse.streaming;

import com.companieswatch.events.ResourceKind;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * One parsed change event from a stream. {@code timepoint} is the resume marker; {@code data} is
 * the changed resource (parsed lazily by the classifier in step 6); {@code rawJson} is the
 * original line, stored on the resulting event for reference.
 */
public record StreamMessage(
        CompaniesHouseStream stream,
        ResourceKind resourceKind,
        String companyNumber,
        String resourceId,
        String resourceUri,
        String eventType,
        long timepoint,
        Instant publishedAt,
        JsonNode data,
        String rawJson) {

    /** The change's publish time, falling back to now if the stream didn't supply one. */
    public Instant occurredAtOrNow() {
        return publishedAt != null ? publishedAt : Instant.now();
    }
}
