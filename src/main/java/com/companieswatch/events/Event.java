package com.companieswatch.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A persisted, relevant change at a watched company. The {@code dedupKey} unique constraint is
 * the idempotency guarantee (architecture.md): the classifier builds a deterministic key, and a
 * duplicate insert fails fast, so the same change is never stored — or alerted on — twice across
 * stream reconnects/redeliveries.
 */
@Entity
@Table(name = "events",
        uniqueConstraints = @UniqueConstraint(name = "ux_events_dedup", columnNames = "dedup_key"))
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_number", nullable = false)
    private String companyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_kind", nullable = false)
    private ResourceKind resourceKind;

    @Column(name = "timepoint")
    private Long timepoint;

    @Column(name = "dedup_key", nullable = false)
    private String dedupKey;

    @Column(nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Event() {
        // for JPA
    }

    public Event(String companyNumber, EventType eventType, ResourceKind resourceKind,
                 String dedupKey, String summary) {
        this.companyNumber = companyNumber;
        this.eventType = eventType;
        this.resourceKind = resourceKind;
        this.dedupKey = dedupKey;
        this.summary = summary;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public EventType getEventType() {
        return eventType;
    }

    public ResourceKind getResourceKind() {
        return resourceKind;
    }

    public Long getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(Long timepoint) {
        this.timepoint = timepoint;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public String getSummary() {
        return summary;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
