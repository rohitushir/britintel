package com.companieswatch.processing;

import com.companieswatch.events.EventType;

/**
 * One alert-worthy change derived from a stream message. {@code discriminator} distinguishes
 * sibling changes that share a timepoint (e.g. a profile message that changes both status and
 * address) so each gets a distinct dedup key.
 */
record ClassifiedChange(EventType eventType, String summary, String discriminator) {
}
