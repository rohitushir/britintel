package com.companieswatch.watchlist;

import com.companieswatch.account.User;
import com.companieswatch.account.UserRepository;
import com.companieswatch.company.CompanyBackfillService;
import com.companieswatch.companieshouse.rest.CompanyProfile;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchListService {

    private final WatchedCompanyRepository watchedCompanyRepository;
    private final UserRepository userRepository;
    private final CompanyBackfillService backfillService;

    public WatchListService(WatchedCompanyRepository watchedCompanyRepository,
                            UserRepository userRepository,
                            CompanyBackfillService backfillService) {
        this.watchedCompanyRepository = watchedCompanyRepository;
        this.userRepository = userRepository;
        this.backfillService = backfillService;
    }

    @Transactional(readOnly = true)
    public List<WatchedCompany> list(Long userId) {
        return watchedCompanyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Add a company to a user's watch list: validate the number, enforce the per-account cap,
     * reject duplicates, then backfill the company's current state (which also gives us the
     * display name to cache). Backfill runs first so we don't persist a watch for a company
     * Companies House doesn't recognise — a {@code CompanyNotFoundException} aborts the add.
     */
    @Transactional
    public WatchedCompany add(Long userId, String rawCompanyNumber) {
        String companyNumber = CompanyNumbers.normalise(rawCompanyNumber);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found: " + userId));

        if (watchedCompanyRepository.findByUserIdAndCompanyNumber(userId, companyNumber).isPresent()) {
            throw new AlreadyWatchingException(companyNumber);
        }
        if (watchedCompanyRepository.countByUserId(userId) >= user.getCompanyCap()) {
            throw new WatchLimitExceededException(user.getCompanyCap());
        }

        CompanyProfile profile = backfillService.backfill(companyNumber);

        WatchedCompany watch = new WatchedCompany(userId, companyNumber);
        watch.setCompanyName(profile.companyName());
        return watchedCompanyRepository.save(watch);
    }

    @Transactional
    public void remove(Long userId, String rawCompanyNumber) {
        String companyNumber = CompanyNumbers.normalise(rawCompanyNumber);
        watchedCompanyRepository.deleteByUserIdAndCompanyNumber(userId, companyNumber);
    }
}
