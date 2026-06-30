package com.companieswatch.companieshouse.rest;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

/**
 * The slice of a Companies House company profile we persist as last-known state for diffing.
 * The structured address and the full profile are carried as raw JSON strings (stored in the
 * {@code jsonb} columns of {@code company_state}); we avoid mapping every CH field.
 */
public record CompanyProfile(
        String companyNumber,
        String companyName,
        String companyStatus,
        LocalDate dateOfCreation,
        String registeredOfficeJson,
        String rawJson) {

    static CompanyProfile from(String companyNumber, JsonNode root, String rawJson) {
        String name = text(root, "company_name");
        String status = text(root, "company_status");
        LocalDate created = parseDate(text(root, "date_of_creation"));
        JsonNode office = root.get("registered_office_address");
        String officeJson = (office != null && !office.isNull()) ? office.toString() : null;
        return new CompanyProfile(companyNumber, name, status, created, officeJson, rawJson);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
