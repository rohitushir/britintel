package com.companieswatch.earlyaccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** A landing-page email capture — one interested visitor (demand signal). */
@Entity
@Table(name = "early_access_signups")
public class EarlyAccessSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    /** Where on the page the click came from (hero, pricing, ...), for attribution. */
    @Column
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EarlyAccessSignup() {
        // for JPA
    }

    public EarlyAccessSignup(String email, String source) {
        this.email = email;
        this.source = source;
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

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
