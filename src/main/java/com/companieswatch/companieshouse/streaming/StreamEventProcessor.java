package com.companieswatch.companieshouse.streaming;

/**
 * Handles each parsed change from the stream. Step 5 ships a logging no-op; step 6 supplies the
 * real implementation (match against watched companies → classify → persist idempotently → alert).
 *
 * <p>Implementations must be quick and must not throw for routine cases — a thrown exception is
 * caught and logged by the connection so the stream is never dropped, but the event is lost.
 */
public interface StreamEventProcessor {

    void process(StreamMessage message);
}
