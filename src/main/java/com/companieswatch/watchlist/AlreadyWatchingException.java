package com.companieswatch.watchlist;

/** Thrown when a user adds a company already on their watch list. */
public class AlreadyWatchingException extends RuntimeException {

    public AlreadyWatchingException(String companyNumber) {
        super("Already watching " + companyNumber);
    }
}
