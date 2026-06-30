package com.companieswatch.watchlist;

/** Thrown when adding a company would exceed the account's watched-company cap. */
public class WatchLimitExceededException extends RuntimeException {

    public WatchLimitExceededException(int cap) {
        super("Watch-list limit reached (" + cap + " companies). Remove one or upgrade.");
    }
}
