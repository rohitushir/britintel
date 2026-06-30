package com.companieswatch.alerts;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real email notifier. Enabled by {@code companieswatch.alerts.email.enabled=true}, at which point
 * a {@link JavaMailSender} must be configured via {@code spring.mail.*} (whichever SMTP / email
 * service is chosen). The {@code from} address comes from {@code companieswatch.alerts.email.from}.
 */
@Component
@ConditionalOnProperty(prefix = "companieswatch.alerts.email", name = "enabled", havingValue = "true")
public class EmailAlertNotifier implements AlertNotifier {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailAlertNotifier(JavaMailSender mailSender,
                              @org.springframework.beans.factory.annotation.Value(
                                      "${companieswatch.alerts.email.from:alerts@britintel.app}")
                              String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
