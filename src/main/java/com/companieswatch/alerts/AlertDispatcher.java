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
    private final AlertContentFactory contentFactory;

    public AlertDispatcher(WatchedCompanyRepository watchedCompanyRepository,
                           UserRepository userRepository,
                           AlertNotifier notifier,
                           AlertContentFactory contentFactory) {
        this.watchedCompanyRepository = watchedCompanyRepository;
        this.userRepository = userRepository;
        this.notifier = notifier;
        this.contentFactory = contentFactory;
    }

    @Async
    @TransactionalEventListener
    public void onRelevantChange(RelevantChangeEvent event) {
        List<WatchedCompany> watchers = watchedCompanyRepository.findByCompanyNumber(event.companyNumber());
        if (watchers.isEmpty()) {
            return;
        }
        AlertContent content = contentFactory.render(event);
        for (WatchedCompany watch : watchers) {
            userRepository.findById(watch.getUserId())
                    .filter(User::isEnabled)
                    .ifPresent(user -> dispatch(user, content));
        }
    }

    private void dispatch(User user, AlertContent content) {
        try {
            notifier.send(user.getEmail(), content);
            log.info("Alert sent to {}: {}", user.getEmail(), content.subject());
        } catch (Exception e) {
            // Don't let one failed send abort the rest; log for retry/observability.
            log.error("Failed to send alert to {}: {}", user.getEmail(), e.toString());
        }
    }
}
