package com.companieswatch.companieshouse.streaming;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persisted resume point for one stream. */
@Entity
@Table(name = "stream_position")
public class StreamPosition {

    @Id
    @Column(name = "stream_name")
    private String streamName;

    @Column(name = "last_timepoint")
    private Long lastTimepoint;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected StreamPosition() {
        // for JPA
    }

    public StreamPosition(String streamName) {
        this.streamName = streamName;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getStreamName() {
        return streamName;
    }

    public Long getLastTimepoint() {
        return lastTimepoint;
    }

    public void setLastTimepoint(Long lastTimepoint) {
        this.lastTimepoint = lastTimepoint;
    }
}
