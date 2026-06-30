package com.companieswatch.companieshouse.rest;

/** Wraps failures talking to the Companies House REST API. */
public class CompaniesHouseException extends RuntimeException {

    public CompaniesHouseException(String message) {
        super(message);
    }

    public CompaniesHouseException(String message, Throwable cause) {
        super(message, cause);
    }
}
