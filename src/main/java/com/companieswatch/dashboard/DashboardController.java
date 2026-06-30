package com.companieswatch.dashboard;

import com.companieswatch.account.AppUserDetails;
import com.companieswatch.events.Event;
import com.companieswatch.events.EventRepository;
import com.companieswatch.events.EventType;
import com.companieswatch.watchlist.WatchedCompanyRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only dashboard feed: the most recent events across the companies the logged-in user
 * watches. Events are stored per company; we scope them to the user by their watch list.
 */
@RestController
@RequestMapping("/api/events")
public class DashboardController {

    private static final int MAX_LIMIT = 200;

    private final EventRepository eventRepository;
    private final WatchedCompanyRepository watchedCompanyRepository;

    public DashboardController(EventRepository eventRepository,
                              WatchedCompanyRepository watchedCompanyRepository) {
        this.eventRepository = eventRepository;
        this.watchedCompanyRepository = watchedCompanyRepository;
    }

    @GetMapping
    public List<EventView> recent(@AuthenticationPrincipal AppUserDetails principal,
                                  @RequestParam(defaultValue = "50") int limit) {
        int capped = Math.clamp(limit, 1, MAX_LIMIT);

        List<String> watched = watchedCompanyRepository.findByUserIdOrderByCreatedAtDesc(principal.getId())
                .stream().map(w -> w.getCompanyNumber()).toList();
        if (watched.isEmpty()) {
            return List.of();
        }

        return eventRepository
                .findByCompanyNumberInOrderByCreatedAtDesc(watched, PageRequest.of(0, capped))
                .stream().map(EventView::of).toList();
    }

    public record EventView(String companyNumber, EventType eventType, String summary,
                            Instant occurredAt, Instant createdAt) {
        static EventView of(Event e) {
            return new EventView(e.getCompanyNumber(), e.getEventType(), e.getSummary(),
                    e.getOccurredAt(), e.getCreatedAt());
        }
    }
}
