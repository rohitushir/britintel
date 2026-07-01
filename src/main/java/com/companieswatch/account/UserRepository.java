package com.companieswatch.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Case-insensitive email lookup (matches the {@code lower(email)} unique index). */
    @Query("select u from User u where lower(u.email) = lower(?1)")
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByClerkUserId(String clerkUserId);
}
