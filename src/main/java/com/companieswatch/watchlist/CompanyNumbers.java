package com.companieswatch.watchlist;

import java.util.regex.Pattern;

/** Normalises and validates Companies House company numbers (8 chars, alphanumeric). */
public final class CompanyNumbers {

    // e.g. "12345678", "SC123456", "OC123456", "NI123456".
    private static final Pattern VALID = Pattern.compile("^[A-Z0-9]{8}$");

    private CompanyNumbers() {
    }

    /**
     * Normalise to the canonical form (trimmed, upper-case, spaces removed).
     *
     * @throws IllegalArgumentException if it is not a valid company-number shape
     */
    public static String normalise(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Company number is required");
        }
        String cleaned = raw.trim().toUpperCase().replace(" ", "");
        if (!VALID.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid company number: " + raw);
        }
        return cleaned;
    }
}
