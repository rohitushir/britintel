package com.companieswatch.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Create an account. The default watched-company cap comes from the entity default (50). */
    @Transactional
    public User register(String email, String rawPassword) {
        String normalised = email == null ? "" : email.trim();
        if (userRepository.existsByEmailIgnoreCase(normalised)) {
            throw new EmailAlreadyExistsException(normalised);
        }
        User user = new User(normalised, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }
}
