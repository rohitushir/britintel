package com.companieswatch.companieshouse.streaming;

import java.util.Iterator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds one resilient connection to a single Companies House stream:
 *
 * <ol>
 *   <li>open from the last persisted timepoint (or live head),</li>
 *   <li>consume line-by-line, hand each parsed event to the processor,</li>
 *   <li>persist the resume timepoint periodically and on disconnect,</li>
 *   <li>on drop/error, reconnect with exponential back-off; on a stale timepoint (416), clear the
 *       position and resume from the head.</li>
 * </ol>
 *
 * Runs on its own thread; {@link #stop()} requests a clean exit.
 */
public class StreamConnection implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(StreamConnection.class);

    private final CompaniesHouseStream stream;
    private final StreamSource source;
    private final StreamMessageParser parser;
    private final StreamEventProcessor processor;
    private final PositionStore positionStore;
    private final long baseDelayMillis;
    private final long maxDelayMillis;
    private final int flushEvery;

    private volatile boolean running = true;
    private Long lastTimepoint;

    public StreamConnection(CompaniesHouseStream stream, StreamSource source,
                            StreamMessageParser parser, StreamEventProcessor processor,
                            PositionStore positionStore, long baseDelayMillis,
                            long maxDelayMillis, int flushEvery) {
        this.stream = stream;
        this.source = source;
        this.parser = parser;
        this.processor = processor;
        this.positionStore = positionStore;
        this.baseDelayMillis = baseDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.flushEvery = Math.max(1, flushEvery);
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        log.info("Stream {} starting", stream.configName());
        long delay = baseDelayMillis;

        while (running) {
            lastTimepoint = positionStore.load(stream);
            log.info("Stream {} connecting from timepoint {}", stream.configName(),
                    lastTimepoint != null ? lastTimepoint : "head");
            try {
                consumeOneConnection();
                // A normal end-of-stream is still a disconnect; fall through to reconnect.
                log.info("Stream {} ended; will reconnect", stream.configName());
            } catch (StaleTimepointException e) {
                log.warn("Stream {} timepoint too old; clearing and resuming from head", stream.configName());
                positionStore.clear(stream);
                lastTimepoint = null;
                delay = baseDelayMillis;
                continue; // reconnect immediately from the head
            } catch (Exception e) {
                log.warn("Stream {} disconnected: {}", stream.configName(), e.toString());
            }

            positionStore.save(stream, lastTimepoint);
            if (!running) {
                break;
            }
            sleep(delay);
            delay = Math.min(maxDelayMillis, delay * 2);
        }

        positionStore.save(stream, lastTimepoint);
        log.info("Stream {} stopped at timepoint {}", stream.configName(), lastTimepoint);
    }

    private void consumeOneConnection() throws Exception {
        int sinceFlush = 0;
        try (Stream<String> lines = source.open(stream, lastTimepoint)) {
            Iterator<String> it = lines.iterator();
            while (running && it.hasNext()) {
                String line = it.next();
                if (line == null || line.isBlank()) {
                    continue; // heartbeat
                }
                StreamMessage message;
                try {
                    message = parser.parse(stream, line);
                } catch (Exception e) {
                    log.warn("Stream {} skipping unparseable line: {}", stream.configName(), e.toString());
                    continue;
                }
                if (message == null) {
                    continue;
                }
                dispatch(message);
                lastTimepoint = message.timepoint();
                if (++sinceFlush >= flushEvery) {
                    positionStore.save(stream, lastTimepoint);
                    sinceFlush = 0;
                }
            }
        }
    }

    private void dispatch(StreamMessage message) {
        try {
            processor.process(message);
        } catch (Exception e) {
            // Never let a processing failure drop the stream; log and advance.
            log.error("Stream {} processing failed for company {} timepoint {}: {}",
                    stream.configName(), message.companyNumber(), message.timepoint(), e.toString());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
