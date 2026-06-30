package com.companieswatch.watchlist;

import com.companieswatch.account.ClerkUserService;
import com.companieswatch.company.CompanyState;
import com.companieswatch.company.CompanyStateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
public class WatchListController {

    private final WatchListService watchListService;
    private final CompanyStateRepository companyStateRepository;
    private final ClerkUserService clerkUserService;

    public WatchListController(WatchListService watchListService,
                              CompanyStateRepository companyStateRepository,
                              ClerkUserService clerkUserService) {
        this.watchListService = watchListService;
        this.companyStateRepository = companyStateRepository;
        this.clerkUserService = clerkUserService;
    }

    @GetMapping
    public List<WatchedCompanyView> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = clerkUserService.resolve(jwt).getId();
        List<WatchedCompany> watched = watchListService.list(userId);
        // Batch-load current status for the watched companies so the UI can flag risk.
        Map<String, String> statusByNumber = companyStateRepository
                .findAllById(watched.stream().map(WatchedCompany::getCompanyNumber).toList())
                .stream()
                .filter(s -> s.getCompanyStatus() != null)
                .collect(Collectors.toMap(CompanyState::getCompanyNumber, CompanyState::getCompanyStatus));
        return watched.stream()
                .map(w -> WatchedCompanyView.of(w, statusByNumber.get(w.getCompanyNumber())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchedCompanyView add(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody AddCompanyRequest request) {
        Long userId = clerkUserService.resolve(jwt).getId();
        WatchedCompany watch = watchListService.add(userId, request.companyNumber());
        String status = companyStateRepository.findById(watch.getCompanyNumber())
                .map(CompanyState::getCompanyStatus)
                .orElse(null);
        return WatchedCompanyView.of(watch, status);
    }

    @DeleteMapping("/{companyNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt,
                       @PathVariable String companyNumber) {
        watchListService.remove(clerkUserService.resolve(jwt).getId(), companyNumber);
    }

    public record AddCompanyRequest(@NotBlank String companyNumber) {
    }

    public record WatchedCompanyView(String companyNumber, String companyName, String status,
                                     Instant addedAt) {
        static WatchedCompanyView of(WatchedCompany w, String status) {
            return new WatchedCompanyView(w.getCompanyNumber(), w.getCompanyName(), status,
                    w.getCreatedAt());
        }
    }
}
