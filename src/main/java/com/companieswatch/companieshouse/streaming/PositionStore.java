package com.companieswatch.companieshouse.streaming;

/** Load/save the resume timepoint for a stream. An interface so the connection loop is testable. */
public interface PositionStore {

    /** Last processed timepoint, or {@code null} to start from the live head of the stream. */
    Long load(CompaniesHouseStream stream);

    void save(CompaniesHouseStream stream, Long timepoint);

    /** Forget the position (e.g. after a 416 — the stored timepoint is too old to resume from). */
    void clear(CompaniesHouseStream stream);
}
