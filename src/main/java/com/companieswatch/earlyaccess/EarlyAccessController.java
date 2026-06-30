package com.companieswatch.earlyaccess;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public early-access capture for the marketing landing page. Storing the email turns each CTA
 * click into a measurable demand signal. Idempotent: re-submitting the same email is a no-op success.
 */
@RestController
@RequestMapping("/api/early-access")
public class EarlyAccessController {

    private final EarlyAccessSignupRepository repository;

    public EarlyAccessController(EarlyAccessSignupRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Map<String, String> join(@Valid @RequestBody JoinRequest request) {
        String email = request.email().trim();
        if (!repository.existsByEmailIgnoreCase(email)) {
            try {
                repository.save(new EarlyAccessSignup(email, request.source()));
            } catch (DataIntegrityViolationException duplicate) {
                // Concurrent submit of the same email — already captured, treat as success.
            }
        }
        return Map.of("status", "ok");
    }

    public record JoinRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @Size(max = 64) String source) {
    }
}
