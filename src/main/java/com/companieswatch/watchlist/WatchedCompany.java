package com.companieswatch.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * One company that one user watches. A company may be watched by many users; matching looks
 * up watchers by {@code companyNumber} (indexed). We store the FK as a plain {@code userId}
 * rather than a JPA association to keep the matcher query simple and avoid lazy-loading users.
 */
@Entity
@Table(name = "watched_companies",
        uniqueConstraints = @UniqueConstraint(name = "ux_watch_user_company",
                columnNames = {"user_id", "company_number"}))
public class WatchedCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_number", nullable = false)
    private String companyNumber;

    /** Cached display name; filled in on backfill (step 3), null until then. */
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WatchedCompany() {
        // for JPA
    }

    public WatchedCompany(Long userId, String companyNumber) {
        this.userId = userId;
        this.companyNumber = companyNumber;
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

    public Long getUserId() {
        return userId;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
