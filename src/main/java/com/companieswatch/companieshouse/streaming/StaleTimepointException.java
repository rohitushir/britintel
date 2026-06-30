package com.companieswatch.companieshouse.streaming;

/**
 * Signals that the stored timepoint is too old for Companies House to resume from (HTTP 416).
 * The connection clears the position and reconnects from the live head.
 */
public class StaleTimepointException extends RuntimeException {

    public StaleTimepointException(String message) {
        super(message);
    }
}
