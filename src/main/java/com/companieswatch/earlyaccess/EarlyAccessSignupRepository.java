package com.companieswatch.earlyaccess;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EarlyAccessSignupRepository extends JpaRepository<EarlyAccessSignup, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
