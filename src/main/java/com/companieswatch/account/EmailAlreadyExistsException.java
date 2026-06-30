package com.companieswatch.account;

/** Thrown when registering an email that already has an account. */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account already exists for " + email);
    }
}
