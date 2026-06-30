package com.companieswatch.account;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the local {@link User} for an authenticated Clerk session token, provisioning one on
 * first sign-in (keyed by the Clerk user id). The email comes from the token if present, otherwise
 * from the Clerk Backend API. If a legacy account already has that email, it is linked rather than
 * duplicated.
 */
@Service
public class ClerkUserService {

    private final UserRepository userRepository;
    private final ClerkBackendClient clerkBackendClient;

    public ClerkUserService(UserRepository userRepository, ClerkBackendClient clerkBackendClient) {
        this.userRepository = userRepository;
        this.clerkBackendClient = clerkBackendClient;
    }

    @Transactional
    public User resolve(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        return userRepository.findByClerkUserId(clerkUserId)
                .orElseGet(() -> provision(jwt, clerkUserId));
    }

    private User provision(Jwt jwt, String clerkUserId) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = clerkBackendClient.fetchPrimaryEmail(clerkUserId);
        }
        String resolvedEmail = email;
        // Link a pre-existing local account with the same email instead of creating a duplicate.
        return userRepository.findByEmailIgnoreCase(resolvedEmail)
                .map(existing -> {
                    existing.setClerkUserId(clerkUserId);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(new User(resolvedEmail, clerkUserId)));
    }
}
