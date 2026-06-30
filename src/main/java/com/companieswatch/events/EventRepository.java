package com.companieswatch.events;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByDedupKey(String dedupKey);

    /** Recent events across a set of company numbers — the dashboard feed for a user. */
    List<Event> findByCompanyNumberInOrderByCreatedAtDesc(
            Collection<String> companyNumbers, Pageable pageable);

    List<Event> findByCompanyNumberOrderByCreatedAtDesc(String companyNumber, Pageable pageable);
}
