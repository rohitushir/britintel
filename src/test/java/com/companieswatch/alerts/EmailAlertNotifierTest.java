package com.companieswatch.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Proves the real notifier sends a multipart (text + HTML) alert over SMTP with the correct
 * envelope and content, using an in-memory GreenMail server (no external provider needed).
 */
class EmailAlertNotifierTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void sendsMultipartAlertWithHtmlAndDeepLink() throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(greenMail.getSmtp().getPort());

        EmailAlertNotifier notifier = new EmailAlertNotifier(sender, "alerts@britintel.app");
        AlertContent content = new AlertContent(
                "[BritIntel] charge created — ACME LTD",
                "A change was detected at a company you watch.",
                "<html><body><h1>ACME LTD</h1>"
                        + "<a href=\"https://find-and-update.company-information.service.gov.uk/company/00000006/charges\">"
                        + "View on Companies House</a></body></html>");

        notifier.send("lender@firm.com", content);

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);

        MimeMessage msg = received[0];
        assertThat(msg.getSubject()).isEqualTo("[BritIntel] charge created — ACME LTD");
        assertThat(msg.getFrom()[0].toString()).isEqualTo("alerts@britintel.app");
        assertThat(msg.getAllRecipients()[0].toString()).isEqualTo("lender@firm.com");
        assertThat(msg.getContentType()).contains("multipart");

        String body = allText(msg);
        assertThat(body).contains("A change was detected at a company you watch."); // plain-text part
        assertThat(body).contains("View on Companies House");                       // html part
        assertThat(body).contains(
                "find-and-update.company-information.service.gov.uk/company/00000006/charges");
    }

    /** Recursively concatenates the decoded text of every part (handles nested multiparts). */
    private static String allText(Part part) throws Exception {
        Object c = part.getContent();
        if (c instanceof String s) {
            return s;
        }
        if (c instanceof Multipart mp) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                sb.append(allText(mp.getBodyPart(i)));
            }
            return sb.toString();
        }
        return "";
    }
}
