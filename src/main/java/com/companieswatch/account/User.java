package com.companieswatch.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A user account. Login/security wiring is added in step 4; this is the persistence shape.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    /** Clerk user id (the session token's {@code sub}); the identity key now that Clerk owns auth. */
    @Column(name = "clerk_user_id")
    private String clerkUserId;

    /** Legacy local-password column; unused now that auth is delegated to Clerk. */
    @Column(name = "password_hash")
    private String passwordHash;

    /** Per-account cap on number of watched companies (pricing tiers; no billing in v1). */
    @Column(name = "company_cap", nullable = false)
    private int companyCap = 50;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
        // for JPA
    }

    /** Provision a Clerk-backed account. */
    public User(String email, String clerkUserId) {
        this.email = email;
        this.clerkUserId = clerkUserId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClerkUserId() {
        return clerkUserId;
    }

    public void setClerkUserId(String clerkUserId) {
        this.clerkUserId = clerkUserId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getCompanyCap() {
        return companyCap;
    }

    public void setCompanyCap(int companyCap) {
        this.companyCap = companyCap;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
