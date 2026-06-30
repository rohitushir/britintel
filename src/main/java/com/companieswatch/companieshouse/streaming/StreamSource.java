package com.companieswatch.companieshouse.streaming;

import java.util.stream.Stream;

/**
 * Opens a stream connection and yields its raw lines. Abstracted behind an interface so the
 * connection loop can be unit-tested without real network I/O.
 */
public interface StreamSource {

    /**
     * Open the stream, resuming after {@code fromTimepoint} (or from the live head if null), and
     * return a lazily-populated stream of raw JSON lines that blocks as data arrives. The returned
     * stream must be closed by the caller.
     *
     * @throws StaleTimepointException if {@code fromTimepoint} is too old (HTTP 416)
     * @throws Exception               on connection/transport failure (triggers reconnect)
     */
    Stream<String> open(CompaniesHouseStream stream, Long fromTimepoint) throws Exception;
}
