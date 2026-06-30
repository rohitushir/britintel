package com.companieswatch.alerts;

import com.companieswatch.account.User;
import com.companieswatch.account.UserRepository;
import com.companieswatch.watchlist.WatchedCompany;
import com.companieswatch.watchlist.WatchedCompanyRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns a persisted {@link RelevantChangeEvent} into one alert per user watching that company.
 *
 * <p>Runs <b>after commit</b> ({@link TransactionalEventListener}) so we never email about an event
 * that rolled back, and <b>asynchronously</b> ({@link Async}) so email I/O never blocks the
 * streaming worker. Idempotency upstream (one persisted event per change) means one email per
 * change per watcher.
 */
@Component
public class AlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);

    private final WatchedCompanyRepository watchedCompanyRepository;
    private final UserRepository userRepository;
    private final AlertNotifier notifier;

    public AlertDispatcher(WatchedCompanyRepository watchedCompanyRepository,
                           UserRepository userRepository,
                           AlertNotifier notifier) {
        this.watchedCompanyRepository = watchedCompanyRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
    }

    @Async
    @TransactionalEventListener
    public void onRelevantChange(RelevantChangeEvent event) {
        List<WatchedCompany> watchers = watchedCompanyRepository.findByCompanyNumber(event.companyNumber());
        if (watchers.isEmpty()) {
            return;
        }
        String subject = subject(event);
        String body = body(event);
        for (WatchedCompany watch : watchers) {
            userRepository.findById(watch.getUserId())
                    .filter(User::isEnabled)
                    .ifPresent(user -> dispatch(user, subject, body));
        }
    }

    private void dispatch(User user, String subject, String body) {
        try {
            notifier.send(user.getEmail(), subject, body);
            log.info("Alert sent to {}: {}", user.getEmail(), subject);
        } catch (Exception e) {
            // Don't let one failed send abort the rest; log for retry/observability.
            log.error("Failed to send alert to {}: {}", user.getEmail(), e.toString());
        }
    }

    private String subject(RelevantChangeEvent e) {
        String name = e.companyName() != null ? e.companyName() : e.companyNumber();
        return "[BritIntel] " + e.eventType().name().replace('_', ' ').toLowerCase()
                + " — " + name;
    }

    private String body(RelevantChangeEvent e) {
        String name = e.companyName() != null ? e.companyName() : e.companyNumber();
        return """
                A change was detected at a company you watch.

                Company: %s (%s)
                Change:  %s
                Detail:  %s
                When:    %s

                View your dashboard: %s
                """.formatted(
                name, e.companyNumber(), e.eventType().name(), e.summary(),
                e.occurredAt(), "/");
    }
}
