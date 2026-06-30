package com.companieswatch.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.companieswatch.alerts.RelevantChangeEvent;
import com.companieswatch.companieshouse.streaming.CompaniesHouseStream;
import com.companieswatch.companieshouse.streaming.StreamMessage;
import com.companieswatch.companieshouse.streaming.StreamMessageParser;
import com.companieswatch.company.CompanyState;
import com.companieswatch.company.CompanyStateRepository;
import com.companieswatch.events.Event;
import com.companieswatch.events.EventRepository;
import com.companieswatch.events.EventType;
import com.companieswatch.watchlist.WatchedCompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class ChangeEventProcessorTest {

    private final WatchedCompanyRepository watched = Mockito.mock(WatchedCompanyRepository.class);
    private final CompanyStateRepository states = Mockito.mock(CompanyStateRepository.class);
    private final EventRepository events = Mockito.mock(EventRepository.class);
    private final ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
    private final StreamMessageParser parser = new StreamMessageParser(new ObjectMapper());

    private ChangeEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ChangeEventProcessor(watched, states, events, publisher);
        when(events.existsByDedupKey(anyString())).thenReturn(false);
        when(events.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private StreamMessage profile(long tp, String company, String status) throws Exception {
        String line = "{\"resource_kind\":\"company-profile\",\"resource_id\":\"" + company + "\","
                + "\"resource_uri\":\"/company/" + company + "\","
                + "\"data\":{\"company_number\":\"" + company + "\",\"company_name\":\"ACME\","
                + "\"company_status\":\"" + status + "\"},"
                + "\"event\":{\"timepoint\":" + tp + ",\"type\":\"changed\"}}";
        return parser.parse(CompaniesHouseStream.COMPANIES, line);
    }

    @Test
    void dropsChangesForUnwatchedCompanies() throws Exception {
        when(watched.existsByCompanyNumber("12345678")).thenReturn(false);

        processor.process(profile(1, "12345678", "active"));

        verify(events, never()).saveAndFlush(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void detectsStatusChangeAgainstStoredStateAndPublishes() throws Exception {
        when(watched.existsByCompanyNumber("12345678")).thenReturn(true);
        CompanyState state = new CompanyState("12345678");
        state.setCompanyStatus("active");
        when(states.findById("12345678")).thenReturn(Optional.of(state));

        processor.process(profile(10, "12345678", "liquidation"));

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(events).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEventType()).isEqualTo(EventType.STATUS_CHANGE);
        assertThat(saved.getValue().getSummary()).contains("active").contains("liquidation");

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(published.capture());
        assertThat(published.getValue()).isInstanceOf(RelevantChangeEvent.class);
    }

    @Test
    void firstSightWithoutBaselineStoresStateAndDoesNotAlert() throws Exception {
        when(watched.existsByCompanyNumber("12345678")).thenReturn(true);
        when(states.findById("12345678")).thenReturn(Optional.empty());

        processor.process(profile(5, "12345678", "active"));

        verify(states).save(any(CompanyState.class)); // baseline stored
        verify(events, never()).saveAndFlush(any());  // but no alert
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void noStatusChangeProducesNoEvent() throws Exception {
        when(watched.existsByCompanyNumber("12345678")).thenReturn(true);
        CompanyState state = new CompanyState("12345678");
        state.setCompanyStatus("active");
        when(states.findById("12345678")).thenReturn(Optional.of(state));

        processor.process(profile(11, "12345678", "active"));

        verify(events, never()).saveAndFlush(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void skipsDuplicatesViaDedupKey() throws Exception {
        when(watched.existsByCompanyNumber("12345678")).thenReturn(true);
        CompanyState state = new CompanyState("12345678");
        state.setCompanyStatus("active");
        when(states.findById("12345678")).thenReturn(Optional.of(state));
        when(events.existsByDedupKey(anyString())).thenReturn(true); // already processed

        processor.process(profile(10, "12345678", "liquidation"));

        verify(events, never()).saveAndFlush(any());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void classifiesChargeFromChargesStream() throws Exception {
        when(watched.existsByCompanyNumber("SC123456")).thenReturn(true);
        String line = "{\"resource_kind\":\"company-charges\",\"resource_id\":\"chg1\","
                + "\"resource_uri\":\"/company/SC123456/charges/chg1\","
                + "\"data\":{\"links\":{\"company\":\"/company/SC123456\"},\"status\":\"outstanding\"},"
                + "\"event\":{\"timepoint\":77,\"type\":\"changed\"}}";
        StreamMessage msg = parser.parse(CompaniesHouseStream.CHARGES, line);

        processor.process(msg);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(events, times(1)).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEventType()).isEqualTo(EventType.CHARGE_CREATED);
    }
}
