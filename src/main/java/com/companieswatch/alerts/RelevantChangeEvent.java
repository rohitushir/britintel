package com.companieswatch.alerts;

import com.companieswatch.events.EventType;
import java.time.Instant;

/**
 * In-process application event published when a relevant, de-duplicated change has been persisted
 * for a watched company. The alert dispatcher consumes it after the transaction commits.
 *
 * <p>This is the clean dispatch seam (architecture.md): today an in-process Spring event; if
 * throughput ever demands it, this is the boundary where an external broker would slot in.
 */
public record RelevantChangeEvent(
        Long eventId,
        String companyNumber,
        String companyName,
        EventType eventType,
        String summary,
        Instant occurredAt) {
}
