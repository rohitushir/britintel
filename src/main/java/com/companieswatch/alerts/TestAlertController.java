package com.companieswatch.alerts;

import com.companieswatch.account.AppUserDetails;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public TestAlertController(TestAlertService testAlertService) {
        this.testAlertService = testAlertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> send(@AuthenticationPrincipal AppUserDetails principal) {
        String companyNumber = testAlertService.fire(principal.getId());
        return Map.of(
                "status", "queued",
                "companyNumber", companyNumber,
                "sentTo", principal.getUsername());
    }
}
