package com.companieswatch.processing;

import com.companieswatch.alerts.RelevantChangeEvent;
import com.companieswatch.companieshouse.streaming.StreamEventProcessor;
import com.companieswatch.companieshouse.streaming.StreamMessage;
import com.companieswatch.company.CompanyState;
import com.companieswatch.company.CompanyStateRepository;
import com.companieswatch.events.Event;
import com.companieswatch.events.EventRepository;
import com.companieswatch.events.EventType;
import com.companieswatch.watchlist.WatchedCompanyRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The real stream processor (architecture.md steps "Matcher" + "Event store"):
 *
 * <ol>
 *   <li><b>Match</b> — drop changes for companies nobody watches (fast indexed lookup).</li>
 *   <li><b>Classify</b> — by stream kind, using a state diff for profile changes, into the
 *       lender event types (status / address / charge / officer / filing).</li>
 *   <li><b>Persist idempotently</b> — a deterministic dedup key + the unique constraint guarantee
 *       no change is stored or alerted on twice across reconnects/redeliveries.</li>
 *   <li><b>Publish</b> — an in-process {@link RelevantChangeEvent} for the alert dispatcher.</li>
 * </ol>
 *
 * Marked {@code @Primary} so it supersedes the step-5 logging processor.
 */
@Service
@Primary
@Transactional
public class ChangeEventProcessor implements StreamEventProcessor {

    private final WatchedCompanyRepository watchedCompanyRepository;
    private final CompanyStateRepository companyStateRepository;
    private final EventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChangeEventProcessor(WatchedCompanyRepository watchedCompanyRepository,
                                CompanyStateRepository companyStateRepository,
                                EventRepository eventRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.watchedCompanyRepository = watchedCompanyRepository;
        this.companyStateRepository = companyStateRepository;
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void process(StreamMessage message) {
        String companyNumber = message.companyNumber();
        if (companyNumber == null || companyNumber.isBlank()) {
            return;
        }
        // 1) Match: ignore the vast majority of the firehose — companies nobody watches.
        if (!watchedCompanyRepository.existsByCompanyNumber(companyNumber)) {
            return;
        }

        // 2) Classify (profile changes also refresh the stored state for future diffs).
        List<ClassifiedChange> changes = classify(message);
        if (changes.isEmpty()) {
            return;
        }

        String companyName = resolveCompanyName(message, companyNumber);
        for (ClassifiedChange change : changes) {
            persistAndPublish(message, companyNumber, companyName, change);
        }
    }

    private void persistAndPublish(StreamMessage message, String companyNumber, String companyName,
                                   ClassifiedChange change) {
        String dedupKey = dedupKey(message, change);
        // 3) Idempotency: cheap pre-check, plus the unique constraint as the hard guarantee.
        if (eventRepository.existsByDedupKey(dedupKey)) {
            return;
        }
        Event event = new Event(companyNumber, change.eventType(), message.resourceKind(),
                dedupKey, change.summary());
        event.setTimepoint(message.timepoint());
        event.setOccurredAt(message.publishedAt());
        event.setPayload(message.rawJson());
        try {
            eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException duplicate) {
            return; // a concurrent/redelivered insert won the race — already handled.
        }

        // 4) Publish for the alert dispatcher (consumed after commit).
        eventPublisher.publishEvent(new RelevantChangeEvent(event.getId(), companyNumber,
                companyName, change.eventType(), change.summary(), message.occurredAtOrNow()));
    }

    private String dedupKey(StreamMessage message, ClassifiedChange change) {
        String discriminator = change.discriminator() != null ? change.discriminator() : "-";
        return String.join("|",
                message.resourceKind().name(),
                change.eventType().name(),
                message.companyNumber(),
                discriminator,
                Long.toString(message.timepoint()));
    }

    // --- Classification ------------------------------------------------------------------------

    private List<ClassifiedChange> classify(StreamMessage message) {
        return switch (message.resourceKind()) {
            case COMPANY_PROFILE -> classifyProfile(message);
            case CHARGES -> classifyCharge(message);
            case OFFICERS -> classifyOfficer(message);
            case FILING_HISTORY -> List.of(new ClassifiedChange(EventType.NEW_FILING,
                    filingSummary(message), message.resourceId()));
        };
    }

    /** Diff the incoming profile against stored state, then refresh the state. */
    private List<ClassifiedChange> classifyProfile(StreamMessage message) {
        JsonNode data = message.data();
        if (data == null) {
            return List.of();
        }
        String companyNumber = message.companyNumber();
        String newName = text(data, "company_name");
        String newStatus = text(data, "company_status");
        JsonNode office = data.get("registered_office_address");
        String newOffice = (office != null && !office.isNull()) ? office.toString() : null;

        CompanyState state = companyStateRepository.findById(companyNumber).orElse(null);
        if (state == null) {
            // No baseline (backfill normally created it) — store and don't alert on first sight.
            state = new CompanyState(companyNumber);
            applyProfile(state, message, newName, newStatus, newOffice);
            companyStateRepository.save(state);
            return List.of();
        }

        List<ClassifiedChange> changes = new ArrayList<>();
        if (newStatus != null && !newStatus.equals(state.getCompanyStatus())) {
            changes.add(new ClassifiedChange(EventType.STATUS_CHANGE,
                    "Status changed: " + orUnknown(state.getCompanyStatus()) + " → " + newStatus,
                    "status"));
        }
        if (newOffice != null && !newOffice.equals(state.getRegisteredOffice())) {
            changes.add(new ClassifiedChange(EventType.ADDRESS_CHANGE,
                    "Registered office address changed", "address"));
        }
        applyProfile(state, message, newName, newStatus, newOffice);
        companyStateRepository.save(state);
        return changes;
    }

    private void applyProfile(CompanyState state, StreamMessage message,
                              String name, String status, String office) {
        if (name != null) {
            state.setCompanyName(name);
        }
        if (status != null) {
            state.setCompanyStatus(status);
        }
        if (office != null) {
            state.setRegisteredOffice(office);
        }
        state.setRawProfile(message.rawJson());
        state.setLastTimepoint(message.timepoint());
    }

    private List<ClassifiedChange> classifyCharge(StreamMessage message) {
        JsonNode data = message.data();
        String status = data != null ? text(data, "status") : null;
        boolean satisfied = status != null
                && (status.equalsIgnoreCase("satisfied") || status.equalsIgnoreCase("fully-satisfied"));
        EventType type = satisfied ? EventType.CHARGE_SATISFIED : EventType.CHARGE_CREATED;
        String summary = satisfied ? "Charge satisfied" : "New charge registered";
        return List.of(new ClassifiedChange(type, summary, message.resourceId()));
    }

    private List<ClassifiedChange> classifyOfficer(StreamMessage message) {
        JsonNode data = message.data();
        String resignedOn = data != null ? text(data, "resigned_on") : null;
        String name = data != null ? text(data, "name") : null;
        EventType type = resignedOn != null ? EventType.OFFICER_RESIGNED : EventType.OFFICER_APPOINTED;
        String verb = resignedOn != null ? "Officer resigned" : "Officer appointed";
        String summary = name != null ? verb + ": " + name : verb;
        return List.of(new ClassifiedChange(type, summary, message.resourceId()));
    }

    private String filingSummary(StreamMessage message) {
        JsonNode data = message.data();
        String description = data != null ? text(data, "description") : null;
        String type = data != null ? text(data, "type") : null;
        String detail = description != null ? description : type;
        return detail != null ? "New filing: " + detail : "New filing";
    }

    private String resolveCompanyName(StreamMessage message, String companyNumber) {
        String fromData = message.data() != null ? text(message.data(), "company_name") : null;
        if (fromData != null) {
            return fromData;
        }
        return companyStateRepository.findById(companyNumber)
                .map(CompanyState::getCompanyName)
                .orElse(null);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static String orUnknown(String value) {
        return value != null ? value : "unknown";
    }
}
