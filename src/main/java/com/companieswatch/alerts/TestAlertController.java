package com.companieswatch.alerts;

import com.companieswatch.account.ClerkUserService;
import com.companieswatch.account.User;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a signed-in user send themselves a test alert to confirm email delivery is configured.
 * Goes through the full production dispatch path; only watchers of the chosen company are emailed
 * (in practice, the caller).
 */
@RestController
@RequestMapping("/api/test-alert")
public class TestAlertController {

    private final TestAlertService testAlertService;
    private final ClerkUserService clerkUserService;

    public TestAlertController(TestAlertService testAlertService, ClerkUserService clerkUserService) {
        this.testAlertService = testAlertService;
        this.clerkUserService = clerkUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> send(@AuthenticationPrincipal Jwt jwt) {
        User user = clerkUserService.resolve(jwt);
        String companyNumber = testAlertService.fire(user.getId());
        return Map.of(
                "status", "queued",
                "companyNumber", companyNumber,
                "sentTo", user.getEmail());
    }
}
