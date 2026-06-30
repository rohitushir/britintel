package com.companieswatch.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Proves the real notifier sends over SMTP with the correct envelope, using an in-memory
 * GreenMail server (no external provider needed). This covers the leg that only runs in
 * production — {@code spring.mail.*} → {@link JavaMailSenderImpl} → SMTP.
 */
class EmailAlertNotifierTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void sendsEmailWithExpectedFromToSubjectAndBody() throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(greenMail.getSmtp().getPort());

        EmailAlertNotifier notifier = new EmailAlertNotifier(sender, "alerts@britintel.app");
        notifier.send("lender@firm.com", "[BritIntel] charge created — ACME LTD",
                "A change was detected at a company you watch.");

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);

        MimeMessage msg = received[0];
        assertThat(msg.getSubject()).isEqualTo("[BritIntel] charge created — ACME LTD");
        assertThat(msg.getFrom()[0].toString()).isEqualTo("alerts@britintel.app");
        assertThat(msg.getAllRecipients()[0].toString()).isEqualTo("lender@firm.com");
        assertThat(GreenMailUtil.getBody(msg))
                .contains("A change was detected at a company you watch.");
    }
}
