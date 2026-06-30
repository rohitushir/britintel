package com.companieswatch.alerts;

/**
 * Sends an alert to a user. v1's only channel is email, but this abstraction lets the concrete
 * provider (SMTP host / transactional-email service) be chosen later without touching the
 * dispatch logic — pick one by configuring {@code companieswatch.alerts.email.enabled} + spring.mail.
 */
public interface AlertNotifier {

    void send(String toEmail, AlertContent content);
}
