package com.companieswatch.events;

/**
 * Which Companies House stream/resource a change originated from (data-sources.md).
 * Used in dedup keys and to route classification.
 */
public enum ResourceKind {
    COMPANY_PROFILE,
    CHARGES,
    OFFICERS,
    FILING_HISTORY
}
