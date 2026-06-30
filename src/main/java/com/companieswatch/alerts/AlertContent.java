package com.companieswatch.alerts;

/**
 * A rendered alert, ready to send: the subject plus both a plain-text and an HTML body (multipart
 * email carries both, so clients that block HTML still show the text). Recipient-independent — the
 * same content goes to every watcher of the company.
 */
public record AlertContent(String subject, String textBody, String htmlBody) {
}
