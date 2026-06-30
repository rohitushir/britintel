package com.companieswatch.account;

import com.companieswatch.watchlist.WatchedCompanyRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;
    private final WatchedCompanyRepository watchedCompanyRepository;

    public AccountController(RegistrationService registrationService,
                            UserRepository userRepository,
                            WatchedCompanyRepository watchedCompanyRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
        this.watchedCompanyRepository = watchedCompanyRepository;
    }

    /** Self-service registration (public). Log in afterwards via the form login at {@code /login}. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisteredView register(@Valid @RequestBody RegisterRequest request) {
        User user = registrationService.register(request.email(), request.password());
        return new RegisteredView(user.getEmail());
    }

    /** Who am I — used by the dashboard to show login state, cap, and usage. */
    @GetMapping("/me")
    public MeView me(@AuthenticationPrincipal AppUserDetails principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Account not found"));
        long watched = watchedCompanyRepository.countByUserId(user.getId());
        return new MeView(user.getEmail(), user.getCompanyCap(), watched);
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record RegisteredView(String email) {
    }

    public record MeView(String email, int companyCap, long watchedCount) {
    }
}
