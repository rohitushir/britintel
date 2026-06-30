package com.companieswatch.watchlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchedCompanyRepository extends JpaRepository<WatchedCompany, Long> {

    /** A user's watch list (dashboard). */
    List<WatchedCompany> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WatchedCompany> findByUserId(Long userId);

    /** Matcher hot path: who watches this company? Backed by ix_watched_company_number. */
    List<WatchedCompany> findByCompanyNumber(String companyNumber);

    /** Distinct set of company numbers anyone watches — the matcher's interest set. */
    @org.springframework.data.jpa.repository.Query(
            "select distinct w.companyNumber from WatchedCompany w")
    List<String> findAllWatchedCompanyNumbers();

    boolean existsByCompanyNumber(String companyNumber);

    Optional<WatchedCompany> findByUserIdAndCompanyNumber(Long userId, String companyNumber);

    long countByUserId(Long userId);

    void deleteByUserIdAndCompanyNumber(Long userId, String companyNumber);
}
