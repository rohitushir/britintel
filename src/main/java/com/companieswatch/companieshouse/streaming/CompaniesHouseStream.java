package com.companieswatch.companieshouse.streaming;

import com.companieswatch.events.ResourceKind;

/**
 * The Companies House streams we can subscribe to, each a separate long-running connection
 * (data-sources.md). Maps a config name to the stream path and the {@link ResourceKind} its
 * events represent.
 */
public enum CompaniesHouseStream {

    COMPANIES("companies", "/companies", ResourceKind.COMPANY_PROFILE),
    CHARGES("charges", "/charges", ResourceKind.CHARGES),
    OFFICERS("officers", "/officers", ResourceKind.OFFICERS),
    FILINGS("filings", "/filings", ResourceKind.FILING_HISTORY);

    private final String configName;
    private final String path;
    private final ResourceKind resourceKind;

    CompaniesHouseStream(String configName, String path, ResourceKind resourceKind) {
        this.configName = configName;
        this.path = path;
        this.resourceKind = resourceKind;
    }

    public String configName() {
        return configName;
    }

    public String path() {
        return path;
    }

    public ResourceKind resourceKind() {
        return resourceKind;
    }

    public static CompaniesHouseStream fromConfig(String name) {
        String key = name == null ? "" : name.trim().toLowerCase();
        for (CompaniesHouseStream s : values()) {
            if (s.configName.equals(key)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown Companies House stream: " + name);
    }
}
