package com.companieswatch.account;

import com.companieswatch.watchlist.WatchedCompanyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final ClerkUserService clerkUserService;
    private final WatchedCompanyRepository watchedCompanyRepository;
    private final String publishableKey;
    private final String frontendApiUrl;

    public AccountController(ClerkUserService clerkUserService,
                            WatchedCompanyRepository watchedCompanyRepository,
                            @Value("${clerk.publishable-key:}") String publishableKey,
                            @Value("${clerk.frontend-api-url:}") String frontendApiUrl) {
        this.clerkUserService = clerkUserService;
        this.watchedCompanyRepository = watchedCompanyRepository;
        this.publishableKey = publishableKey;
        this.frontendApiUrl = frontendApiUrl;
    }

    /** Public bootstrap: the SPA needs the Clerk publishable key to initialise sign-in. */
    @GetMapping("/config")
    public ClerkConfig config() {
        return new ClerkConfig(publishableKey, frontendApiUrl);
    }

    /** Who am I — provisions the local account on first call, returns cap + usage. */
    @GetMapping("/me")
    public MeView me(@AuthenticationPrincipal Jwt jwt) {
        User user = clerkUserService.resolve(jwt);
        long watched = watchedCompanyRepository.countByUserId(user.getId());
        return new MeView(user.getEmail(), user.getCompanyCap(), watched);
    }

    public record ClerkConfig(String publishableKey, String frontendApiUrl) {
    }

    public record MeView(String email, int companyCap, long watchedCount) {
    }
}
