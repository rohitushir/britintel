package com.companieswatch.companieshouse.streaming;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class StreamConnectionTest {

    private final StreamMessageParser parser = new StreamMessageParser(new ObjectMapper());

    private static String line(long timepoint, String company) {
        return "{\"resource_kind\":\"company-profile\",\"resource_id\":\"" + company + "\","
                + "\"resource_uri\":\"/company/" + company + "\","
                + "\"data\":{\"company_number\":\"" + company + "\",\"company_status\":\"active\"},"
                + "\"event\":{\"timepoint\":" + timepoint + ",\"type\":\"changed\"}}";
    }

    /** In-memory store so the loop is testable without a database. */
    private static final class InMemoryPositionStore implements PositionStore {
        private final Map<CompaniesHouseStream, Long> positions = new EnumMap<>(CompaniesHouseStream.class);

        @Override public Long load(CompaniesHouseStream s) { return positions.get(s); }
        @Override public void save(CompaniesHouseStream s, Long tp) { if (tp != null) positions.put(s, tp); }
        @Override public void clear(CompaniesHouseStream s) { positions.remove(s); }
    }

    @Test
    void processesEventsAndPersistsLastTimepoint() {
        var store = new InMemoryPositionStore();
        var captured = new ArrayList<StreamMessage>();
        var holder = new StreamConnection[1];

        StreamEventProcessor processor = msg -> {
            captured.add(msg);
            if (captured.size() >= 2) {
                holder[0].stop();
            }
        };
        StreamSource source = (stream, from) -> Stream.of(line(100, "11111111"), line(101, "22222222"));

        var conn = new StreamConnection(CompaniesHouseStream.COMPANIES, source, parser, processor,
                store, 1, 1, 1);
        holder[0] = conn;
        conn.run();

        assertThat(captured).hasSize(2);
        assertThat(captured.get(0).companyNumber()).isEqualTo("11111111");
        assertThat(store.load(CompaniesHouseStream.COMPANIES)).isEqualTo(101L);
    }

    @Test
    void reconnectsAfterAFailureAndResumes() {
        var store = new InMemoryPositionStore();
        var captured = new ArrayList<StreamMessage>();
        var holder = new StreamConnection[1];
        AtomicInteger attempts = new AtomicInteger();

        StreamEventProcessor processor = msg -> {
            captured.add(msg);
            holder[0].stop();
        };
        StreamSource source = (stream, from) -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IOException("connection dropped");
            }
            return Stream.of(line(200, "33333333"));
        };

        var conn = new StreamConnection(CompaniesHouseStream.COMPANIES, source, parser, processor,
                store, 1, 1, 1);
        holder[0] = conn;
        conn.run();

        assertThat(attempts.get()).isGreaterThanOrEqualTo(2); // failed once, then succeeded
        assertThat(captured).hasSize(1);
        assertThat(store.load(CompaniesHouseStream.COMPANIES)).isEqualTo(200L);
    }

    @Test
    void clearsPositionOnStaleTimepoint() {
        var store = new InMemoryPositionStore();
        store.save(CompaniesHouseStream.COMPANIES, 5L);
        var captured = new ArrayList<StreamMessage>();
        var holder = new StreamConnection[1];
        List<Long> openedFrom = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();

        StreamEventProcessor processor = msg -> {
            captured.add(msg);
            holder[0].stop();
        };
        StreamSource source = (stream, from) -> {
            openedFrom.add(from);
            if (attempts.getAndIncrement() == 0) {
                throw new StaleTimepointException("too old");
            }
            return Stream.of(line(300, "44444444"));
        };

        var conn = new StreamConnection(CompaniesHouseStream.COMPANIES, source, parser, processor,
                store, 1, 1, 1);
        holder[0] = conn;
        conn.run();

        // first open used the stored timepoint 5; after 416 the position was cleared, so the
        // second open started from the head (null).
        assertThat(openedFrom.get(0)).isEqualTo(5L);
        assertThat(openedFrom.get(1)).isNull();
        assertThat(captured).hasSize(1);
    }
}
