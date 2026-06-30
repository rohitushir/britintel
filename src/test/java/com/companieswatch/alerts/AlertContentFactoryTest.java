package com.companieswatch.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import com.companieswatch.events.EventType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertContentFactoryTest {

    private static final String CH = "https://find-and-update.company-information.service.gov.uk";
    private final AlertContentFactory factory = new AlertContentFactory(CH, "");

    private RelevantChangeEvent event(String number, String name, EventType type, String summary) {
        return new RelevantChangeEvent(1L, number, name, type, summary, Instant.parse("2026-06-30T20:00:00Z"));
    }

    @Test
    void chargeEventDeepLinksToTheChargesPage() {
        AlertContent c = factory.render(event("00000006", "ACME LTD", EventType.CHARGE_CREATED,
                "New charge registered"));

        assertThat(c.subject()).isEqualTo("[BritIntel] charge created — ACME LTD");
        assertThat(c.htmlBody()).contains(CH + "/company/00000006/charges");
        assertThat(c.htmlBody()).contains("View on Companies House");
        assertThat(c.textBody()).contains(CH + "/company/00000006/charges");
    }

    @Test
    void officerEventDeepLinksToOfficers() {
        AlertContent c = factory.render(event("SC123456", "Beta Ltd", EventType.OFFICER_RESIGNED,
                "Officer resigned: Jane Doe"));
        assertThat(c.htmlBody()).contains(CH + "/company/SC123456/officers");
    }

    @Test
    void statusAndAddressEventsLinkToTheCompanyPage() {
        AlertContent status = factory.render(event("00000006", "ACME LTD", EventType.STATUS_CHANGE,
                "Status changed: active → liquidation"));
        assertThat(status.htmlBody()).contains(CH + "/company/00000006\"");
        assertThat(status.htmlBody()).doesNotContain("/company/00000006/charges");
    }

    @Test
    void filingEventDeepLinksToFilingHistory() {
        AlertContent c = factory.render(event("00000006", "ACME LTD", EventType.NEW_FILING,
                "New filing: confirmation statement"));
        assertThat(c.htmlBody()).contains(CH + "/company/00000006/filing-history");
    }

    @Test
    void escapesHtmlInCompanyNameAndSummary() {
        AlertContent c = factory.render(event("00000006", "Smith & <b>Co</b> Ltd",
                EventType.STATUS_CHANGE, "Status changed: active → \"dormant\""));
        assertThat(c.htmlBody()).contains("Smith &amp; &lt;b&gt;Co&lt;/b&gt; Ltd");
        assertThat(c.htmlBody()).doesNotContain("<b>Co</b>");
    }
}
