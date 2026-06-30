package com.companieswatch.processing;

import static org.assertj.core.api.Assertions.assertThat;

import com.companieswatch.account.User;
import com.companieswatch.account.UserRepository;
import com.companieswatch.companieshouse.streaming.CompaniesHouseStream;
import com.companieswatch.companieshouse.streaming.StreamMessage;
import com.companieswatch.companieshouse.streaming.StreamMessageParser;
import com.companieswatch.company.CompanyState;
import com.companieswatch.company.CompanyStateRepository;
import com.companieswatch.events.EventRepository;
import com.companieswatch.events.EventType;
import com.companieswatch.watchlist.WatchedCompany;
import com.companieswatch.watchlist.WatchedCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Drives the real processor against the real database to verify classification, persistence, and
 * idempotency through the actual unique constraint (not mocks).
 */
@SpringBootTest
class PipelinePersistenceTest {

    @Autowired ChangeEventProcessor processor;
    @Autowired StreamMessageParser parser;
    @Autowired UserRepository users;
    @Autowired WatchedCompanyRepository watched;
    @Autowired CompanyStateRepository states;
    @Autowired EventRepository events;

    @BeforeEach
    void setUp() {
        events.deleteAll();
        watched.deleteAll();
        states.deleteAll();
        users.deleteAll();

        User user = users.save(new User("lender@firm.com", "x"));
        watched.save(new WatchedCompany(user.getId(), "12345678"));
        CompanyState state = new CompanyState("12345678");
        state.setCompanyStatus("active");
        states.save(state);
    }

    private StreamMessage statusChange(long timepoint) throws Exception {
        String line = "{\"resource_kind\":\"company-profile\",\"resource_id\":\"12345678\","
                + "\"resource_uri\":\"/company/12345678\","
                + "\"data\":{\"company_number\":\"12345678\",\"company_name\":\"ACME\","
                + "\"company_status\":\"liquidation\"},"
                + "\"event\":{\"timepoint\":" + timepoint + ",\"type\":\"changed\"}}";
        return parser.parse(CompaniesHouseStream.COMPANIES, line);
    }

    @Test
    void persistsClassifiedEventAndIsIdempotentOnRedelivery() throws Exception {
        StreamMessage message = statusChange(10);

        processor.process(message);
        assertThat(events.findAll()).hasSize(1);
        assertThat(events.findAll().get(0).getEventType()).isEqualTo(EventType.STATUS_CHANGE);

        // Simulate a stream redelivery of the SAME message (same timepoint) after a resume:
        // reset the state so the diff would re-detect the change, but dedup must still suppress it.
        CompanyState state = states.findById("12345678").orElseThrow();
        state.setCompanyStatus("active");
        states.save(state);

        processor.process(message);
        assertThat(events.findAll()).hasSize(1); // no duplicate
    }
}
