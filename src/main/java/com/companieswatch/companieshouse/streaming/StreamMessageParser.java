package com.companieswatch.companieshouse.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parses a single newline-delimited JSON line from a Companies House stream into a
 * {@link StreamMessage}. Returns {@code null} for heartbeats / lines without an event timepoint.
 */
@Component
public class StreamMessageParser {

    private static final Pattern COMPANY_IN_URI = Pattern.compile("/company/([A-Z0-9]+)");

    private final ObjectMapper objectMapper;

    public StreamMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** @throws com.fasterxml.jackson.core.JsonProcessingException if the line is not valid JSON */
    public StreamMessage parse(CompaniesHouseStream stream, String line) throws Exception {
        JsonNode root = objectMapper.readTree(line);
        JsonNode event = root.path("event");
        JsonNode timepointNode = event.path("timepoint");
        if (!timepointNode.isNumber()) {
            return null; // heartbeat or non-event frame
        }

        String resourceId = text(root, "resource_id");
        String resourceUri = text(root, "resource_uri");
        JsonNode data = root.path("data");
        String companyNumber = extractCompanyNumber(data, resourceUri, resourceId, stream);

        return new StreamMessage(
                stream,
                stream.resourceKind(),
                companyNumber,
                resourceId,
                resourceUri,
                text(event, "type"),
                timepointNode.asLong(),
                parseInstant(text(event, "published_at")),
                data.isMissingNode() ? null : data,
                line);
    }

    private static String extractCompanyNumber(JsonNode data, String resourceUri,
                                               String resourceId, CompaniesHouseStream stream) {
        // 1) explicit field on the resource
        String fromData = text(data, "company_number");
        if (fromData != null) {
            return fromData;
        }
        // 2) the company link, e.g. data.links.company = "/company/12345678"
        String link = text(data.path("links"), "company");
        String fromLink = matchCompany(link);
        if (fromLink != null) {
            return fromLink;
        }
        // 3) the resource URI
        String fromUri = matchCompany(resourceUri);
        if (fromUri != null) {
            return fromUri;
        }
        // 4) on the company-profile stream the resource id IS the company number
        if (stream == CompaniesHouseStream.COMPANIES) {
            return resourceId;
        }
        return null;
    }

    private static String matchCompany(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = COMPANY_IN_URI.matcher(value);
        return m.find() ? m.group(1) : null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (RuntimeException ignored) {
            // not offset-qualified
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (RuntimeException ignored) {
            // not a local date-time
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
