package com.companieswatch.alerts;

import com.companieswatch.events.EventType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Renders a {@link RelevantChangeEvent} into a clear alert email: a subject line, a plain-text
 * fallback, and a branded HTML body with a direct deep link to the relevant Companies House page
 * (the charges page for a charge, officers page for an officer change, and so on).
 */
@Component
public class AlertContentFactory {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final String companiesHouseBaseUrl;
    private final String appBaseUrl;

    public AlertContentFactory(
            @Value("${companieswatch.alerts.companies-house-base-url:https://find-and-update.company-information.service.gov.uk}")
            String companiesHouseBaseUrl,
            @Value("${companieswatch.alerts.app-base-url:}") String appBaseUrl) {
        this.companiesHouseBaseUrl = stripTrailingSlash(companiesHouseBaseUrl);
        this.appBaseUrl = stripTrailingSlash(appBaseUrl);
    }

    public AlertContent render(RelevantChangeEvent e) {
        String name = e.companyName() != null ? e.companyName() : e.companyNumber();
        String typeWords = e.eventType().name().replace('_', ' ').toLowerCase();
        String when = e.occurredAt() != null ? WHEN.format(e.occurredAt()) : WHEN.format(Instant.now());
        String link = companiesHouseLink(e.companyNumber(), e.eventType());

        String subject = "[BritIntel] " + typeWords + " — " + name;
        return new AlertContent(subject, textBody(e, name, typeWords, when, link),
                htmlBody(e, name, typeWords, when, link));
    }

    private String companiesHouseLink(String companyNumber, EventType type) {
        String suffix = switch (type) {
            case CHARGE_CREATED, CHARGE_SATISFIED -> "/charges";
            case OFFICER_APPOINTED, OFFICER_RESIGNED -> "/officers";
            case NEW_FILING -> "/filing-history";
            case STATUS_CHANGE, ADDRESS_CHANGE -> "";
        };
        return companiesHouseBaseUrl + "/company/" + companyNumber + suffix;
    }

    private String textBody(RelevantChangeEvent e, String name, String typeWords, String when,
                            String link) {
        StringBuilder sb = new StringBuilder()
                .append("A change was detected at a company you watch.\n\n")
                .append("Company: ").append(name).append(" (").append(e.companyNumber()).append(")\n")
                .append("Change:  ").append(typeWords).append("\n")
                .append("Detail:  ").append(e.summary()).append("\n")
                .append("When:    ").append(when).append("\n\n")
                .append("View on Companies House: ").append(link).append('\n');
        if (!appBaseUrl.isBlank()) {
            sb.append("Your dashboard: ").append(appBaseUrl).append("/app\n");
        }
        return sb.toString();
    }

    private String htmlBody(RelevantChangeEvent e, String name, String typeWords, String when,
                            String link) {
        String[] badge = badgeColours(e.eventType());
        String dashboard = appBaseUrl.isBlank() ? ""
                : " <a href=\"" + appBaseUrl + "/app\" style=\"color:#1d4ed8;\">View your dashboard</a>.";
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f6f7f9;font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f6f7f9;padding:24px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border:1px solid #e6e8ec;border-radius:14px;overflow:hidden;">
                        <tr><td style="background:#1d4ed8;padding:16px 24px;color:#ffffff;font-weight:700;font-size:16px;">&#9673; BritIntel</td></tr>
                        <tr><td style="padding:24px;">
                          <span style="display:inline-block;background:%s;color:%s;font-size:12px;font-weight:700;padding:3px 10px;border-radius:999px;text-transform:capitalize;">%s</span>
                          <h1 style="font-size:20px;line-height:1.25;margin:14px 0 4px;color:#0f172a;">%s</h1>
                          <p style="margin:0 0 18px;color:#64748b;font-size:13px;">Company %s &middot; %s</p>
                          <p style="margin:0 0 22px;color:#334155;font-size:15px;line-height:1.5;">%s</p>
                          <a href="%s" style="display:inline-block;background:#1d4ed8;color:#ffffff;text-decoration:none;font-weight:600;padding:11px 18px;border-radius:9px;font-size:14px;">View on Companies House &rarr;</a>
                        </td></tr>
                        <tr><td style="padding:16px 24px;border-top:1px solid #eef0f3;color:#94a3b8;font-size:12px;line-height:1.5;">
                          You're receiving this because you watch this company on BritIntel.%s
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                badge[0], badge[1], esc(typeWords), esc(name), esc(e.companyNumber()), esc(when),
                esc(e.summary()), esc(link), dashboard);
    }

    /** Badge background + text colour by lender priority (matches the dashboard). */
    private String[] badgeColours(EventType type) {
        return switch (type) {
            case STATUS_CHANGE, CHARGE_CREATED -> new String[]{"#fef2f2", "#b91c1c"};
            case CHARGE_SATISFIED, OFFICER_APPOINTED, OFFICER_RESIGNED, ADDRESS_CHANGE ->
                    new String[]{"#fffbeb", "#b45309"};
            case NEW_FILING -> new String[]{"#f1f5f9", "#64748b"};
        };
    }

    private static String stripTrailingSlash(String url) {
        return url == null ? "" : url.replaceAll("/+$", "");
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
