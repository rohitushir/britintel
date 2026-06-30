package com.companieswatch.company;

import com.companieswatch.companieshouse.rest.CompaniesHouseRestClient;
import com.companieswatch.companieshouse.rest.CompanyProfile;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfill: when a company enters anyone's watch list, fetch its current state from the REST API
 * and store it as the baseline {@link CompanyState} (architecture.md step 3). The stored state is
 * later diffed against incoming stream changes to detect what actually changed.
 *
 * <p>State is keyed by company number and shared across all watchers, so backfilling is a no-op
 * refresh if another user already watches the same company.
 */
@Service
public class CompanyBackfillService {

    private final CompaniesHouseRestClient restClient;
    private final CompanyStateRepository companyStateRepository;

    public CompanyBackfillService(CompaniesHouseRestClient restClient,
                                  CompanyStateRepository companyStateRepository) {
        this.restClient = restClient;
        this.companyStateRepository = companyStateRepository;
    }

    /**
     * Fetch the current profile and upsert {@link CompanyState}. Returns the fetched profile so
     * callers (e.g. the watch-list add flow) can cache the display name.
     */
    @Transactional
    public CompanyProfile backfill(String companyNumber) {
        CompanyProfile profile = restClient.fetchProfile(companyNumber);

        CompanyState state = companyStateRepository.findById(companyNumber)
                .orElseGet(() -> new CompanyState(companyNumber));
        state.setCompanyName(profile.companyName());
        state.setCompanyStatus(profile.companyStatus());
        state.setRegisteredOffice(profile.registeredOfficeJson());
        state.setDateOfCreation(profile.dateOfCreation());
        state.setRawProfile(profile.rawJson());
        state.setFetchedAt(Instant.now());
        companyStateRepository.save(state);

        return profile;
    }
}
