package com.companieswatch.companieshouse.rest;

/** Thrown when Companies House has no company for the given number (HTTP 404). */
public class CompanyNotFoundException extends CompaniesHouseException {

    private final String companyNumber;

    public CompanyNotFoundException(String companyNumber) {
        super("No company found for number " + companyNumber);
        this.companyNumber = companyNumber;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }
}
