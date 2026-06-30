package com.companieswatch.companieshouse.streaming;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default processor used until step 6 plugs in matching/classification. Counts and trace-logs
 * events so the worker is runnable and observable on its own. Step 6's real processor is marked
 * {@code @Primary} so it takes precedence over this one.
 */
@Component
public class LoggingStreamEventProcessor implements StreamEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(LoggingStreamEventProcessor.class);

    private final AtomicLong seen = new AtomicLong();

    @Override
    public void process(StreamMessage message) {
        long n = seen.incrementAndGet();
        if (log.isTraceEnabled()) {
            log.trace("stream={} timepoint={} company={} type={}",
                    message.stream().configName(), message.timepoint(),
                    message.companyNumber(), message.eventType());
        }
        if (n % 1000 == 0) {
            log.info("Consumed {} stream events (no matcher wired yet)", n);
        }
    }
}
