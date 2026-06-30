package com.companieswatch.watchlist;

import com.companieswatch.account.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public WatchListController(WatchListService watchListService) {
        this.watchListService = watchListService;
    }

    @GetMapping
    public List<WatchedCompanyView> list(@AuthenticationPrincipal AppUserDetails principal) {
        return watchListService.list(principal.getId()).stream()
                .map(WatchedCompanyView::of)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchedCompanyView add(@AuthenticationPrincipal AppUserDetails principal,
                                  @Valid @RequestBody AddCompanyRequest request) {
        return WatchedCompanyView.of(watchListService.add(principal.getId(), request.companyNumber()));
    }

    @DeleteMapping("/{companyNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable String companyNumber) {
        watchListService.remove(principal.getId(), companyNumber);
    }

    public record AddCompanyRequest(@NotBlank String companyNumber) {
    }

    public record WatchedCompanyView(String companyNumber, String companyName, Instant addedAt) {
        static WatchedCompanyView of(WatchedCompany w) {
            return new WatchedCompanyView(w.getCompanyNumber(), w.getCompanyName(), w.getCreatedAt());
        }
    }
}
