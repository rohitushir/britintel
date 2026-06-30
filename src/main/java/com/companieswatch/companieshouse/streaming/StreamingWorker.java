package com.companieswatch.companieshouse.streaming;

import com.companieswatch.config.CompaniesHouseProperties;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Owns the streaming connections. On application ready it starts one {@link StreamConnection} per
 * configured stream (each a daemon thread), enforcing the Companies House two-connection cap, and
 * stops them cleanly on shutdown.
 *
 * <p>This is the single stateful, singleton component of the system: only one instance should
 * hold the stream. It is deliberately isolated from the stateless web layer so deployment can
 * treat them differently later (architecture.md).
 */
@Component
public class StreamingWorker {

    private static final Logger log = LoggerFactory.getLogger(StreamingWorker.class);
    private static final int MAX_CONNECTIONS = 2; // Companies House per-account cap

    private final CompaniesHouseProperties properties;
    private final StreamSource source;
    private final StreamMessageParser parser;
    private final StreamEventProcessor processor;
    private final PositionStore positionStore;

    private final List<StreamConnection> connections = new ArrayList<>();
    private ExecutorService executor;

    public StreamingWorker(CompaniesHouseProperties properties, StreamSource source,
                           StreamMessageParser parser, StreamEventProcessor processor,
                           PositionStore positionStore) {
        this.properties = properties;
        this.source = source;
        this.parser = parser;
        this.processor = processor;
        this.positionStore = positionStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        var config = properties.getStreaming();
        if (!config.isEnabled()) {
            log.info("Streaming worker disabled (companieshouse.streaming.enabled=false)");
            return;
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Streaming worker not started: no Companies House streaming API key configured");
            return;
        }

        List<CompaniesHouseStream> streams = config.getStreams().stream()
                .map(CompaniesHouseStream::fromConfig)
                .distinct()
                .toList();
        if (streams.isEmpty()) {
            log.warn("Streaming worker not started: no streams configured");
            return;
        }
        if (streams.size() > MAX_CONNECTIONS) {
            throw new IllegalStateException("Companies House allows at most " + MAX_CONNECTIONS
                    + " concurrent streaming connections; configured " + streams.size() + ": " + streams);
        }

        executor = Executors.newFixedThreadPool(streams.size(), runnable -> {
            Thread t = new Thread(runnable, "ch-stream");
            t.setDaemon(true);
            return t;
        });

        for (CompaniesHouseStream stream : streams) {
            StreamConnection connection = new StreamConnection(stream, source, parser, processor,
                    positionStore, config.getReconnectBaseDelayMillis(),
                    config.getReconnectMaxDelayMillis(), config.getFlushEvery());
            connections.add(connection);
            executor.submit(connection);
        }
        log.info("Streaming worker started {} connection(s): {}", streams.size(), streams);
    }

    @PreDestroy
    public synchronized void stop() {
        connections.forEach(StreamConnection::stop);
        if (executor != null) {
            executor.shutdownNow();
        }
        if (source instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // best-effort on shutdown
            }
        }
    }
}
