package com.companieswatch.alerts;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    public void send(String toEmail, AlertContent content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            // multipart so the plain-text body is the fallback when HTML is blocked.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(content.subject());
            helper.setText(content.textBody(), content.htmlBody());
        } catch (MessagingException e) {
            throw new MailSendException("Failed to build alert email for " + toEmail, e);
        }
        mailSender.send(message);
    }
}
