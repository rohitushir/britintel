package com.companieswatch.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The last-known relevant state of a company, keyed by company number. Backbone of the
 * state-diff model (architecture.md): when a profile change arrives on the stream we compare
 * it to this row to detect what actually changed (status vs address vs name) before classifying.
 *
 * <p>JSONB columns hold raw/structured payloads as JSON strings; we keep them opaque here and
 * parse on demand in the classifier rather than mapping every Companies House field.
 */
@Entity
@Table(name = "company_state")
public class CompanyState {

    @Id
    @Column(name = "company_number")
    private String companyNumber;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_status")
    private String companyStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "registered_office")
    private String registeredOffice;

    @Column(name = "date_of_creation")
    private LocalDate dateOfCreation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_profile")
    private String rawProfile;

    @Column(name = "last_timepoint")
    private Long lastTimepoint;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CompanyState() {
        // for JPA
    }

    public CompanyState(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public String getCompanyStatus() {
        return companyStatus;
    }

    public void setCompanyStatus(String companyStatus) {
        this.companyStatus = companyStatus;
    }

    public String getRegisteredOffice() {
        return registeredOffice;
    }

    public void setRegisteredOffice(String registeredOffice) {
        this.registeredOffice = registeredOffice;
    }

    public LocalDate getDateOfCreation() {
        return dateOfCreation;
    }

    public void setDateOfCreation(LocalDate dateOfCreation) {
        this.dateOfCreation = dateOfCreation;
    }

    public String getRawProfile() {
        return rawProfile;
    }

    public void setRawProfile(String rawProfile) {
        this.rawProfile = rawProfile;
    }

    public Long getLastTimepoint() {
        return lastTimepoint;
    }

    public void setLastTimepoint(Long lastTimepoint) {
        this.lastTimepoint = lastTimepoint;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
