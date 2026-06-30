package com.companieswatch.companieshouse.streaming;

import static org.assertj.core.api.Assertions.assertThat;

import com.companieswatch.events.ResourceKind;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class StreamMessageParserTest {

    private final StreamMessageParser parser = new StreamMessageParser(new ObjectMapper());

    @Test
    void parsesCompanyProfileEvent() throws Exception {
        String line = """
                {"resource_kind":"company-profile","resource_id":"12345678",
                 "resource_uri":"/company/12345678",
                 "data":{"company_number":"12345678","company_status":"liquidation"},
                 "event":{"timepoint":987,"type":"changed","published_at":"2024-03-01T09:30:00Z"}}
                """;
        StreamMessage msg = parser.parse(CompaniesHouseStream.COMPANIES, line);

        assertThat(msg).isNotNull();
        assertThat(msg.resourceKind()).isEqualTo(ResourceKind.COMPANY_PROFILE);
        assertThat(msg.companyNumber()).isEqualTo("12345678");
        assertThat(msg.timepoint()).isEqualTo(987L);
        assertThat(msg.eventType()).isEqualTo("changed");
        assertThat(msg.publishedAt()).isNotNull();
    }

    @Test
    void extractsCompanyNumberFromChargesResourceUri() throws Exception {
        // charges events carry the charge id as resource_id; the company is in the URI/links.
        String line = """
                {"resource_kind":"company-charges","resource_id":"abc",
                 "resource_uri":"/company/SC123456/charges/abc",
                 "data":{"links":{"company":"/company/SC123456"}},
                 "event":{"timepoint":42,"type":"changed"}}
                """;
        StreamMessage msg = parser.parse(CompaniesHouseStream.CHARGES, line);

        assertThat(msg.companyNumber()).isEqualTo("SC123456");
        assertThat(msg.resourceKind()).isEqualTo(ResourceKind.CHARGES);
    }

    @Test
    void returnsNullForHeartbeatWithoutTimepoint() throws Exception {
        assertThat(parser.parse(CompaniesHouseStream.COMPANIES, "{}")).isNull();
    }
}
