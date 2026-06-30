package com.companieswatch.alerts;

import com.companieswatch.events.EventType;
import com.companieswatch.watchlist.WatchListService;
import com.companieswatch.watchlist.WatchedCompany;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fires a synthetic {@link RelevantChangeEvent} through the real dispatch path so a user can confirm
 * end-to-end alert delivery (event → {@link AlertDispatcher} → {@link AlertNotifier} → inbox).
 *
 * <p>Published inside a transaction so the {@code @TransactionalEventListener} on the dispatcher
 * fires on commit, identical to a real change. It targets a company the user already watches, so the
 * dispatcher's watcher lookup resolves to that user — no special-casing of the alert path.
 */
@Service
public class TestAlertService {

    private final WatchListService watchListService;
    private final ApplicationEventPublisher publisher;

    public TestAlertService(WatchListService watchListService, ApplicationEventPublisher publisher) {
        this.watchListService = watchListService;
        this.publisher = publisher;
    }

    /** Returns the company number the test alert was raised against. */
    @Transactional
    public String fire(Long userId) {
        List<WatchedCompany> watched = watchListService.list(userId);
        if (watched.isEmpty()) {
            throw new IllegalArgumentException(
                    "Add a company to your watchlist first, then send a test alert.");
        }
        WatchedCompany target = watched.get(0);
        publisher.publishEvent(new RelevantChangeEvent(
                null,
                target.getCompanyNumber(),
                target.getCompanyName(),
                EventType.CHARGE_CREATED,
                "Test alert — confirming your BritIntel email delivery is working.",
                Instant.now()));
        return target.getCompanyNumber();
    }
}
