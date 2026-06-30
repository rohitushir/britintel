package com.companieswatch.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default notifier: logs the alert instead of sending it. Active until a real email provider is
 * configured ({@code companieswatch.alerts.email.enabled=true}). Lets the whole pipeline run and
 * be verified end-to-end before an SMTP provider is chosen.
 */
@Component
@ConditionalOnProperty(prefix = "companieswatch.alerts.email", name = "enabled",
        havingValue = "false", matchIfMissing = true)
public class LoggingAlertNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertNotifier.class);

    @Override
    public void send(String toEmail, AlertContent content) {
        log.info("[ALERT → {}] {}", toEmail, content.subject());
        log.debug("Alert body to {}:\n{}", toEmail, content.textBody());
    }
}
