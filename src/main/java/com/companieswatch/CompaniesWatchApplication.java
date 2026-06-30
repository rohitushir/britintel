package com.companieswatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Companies House monitoring tool.
 *
 * <p>v1 runs the whole system in a single Spring Boot process: the dashboard REST/web layer
 * and the (later) streaming worker. They are kept in separate packages so the worker can be
 * split into its own deployable down the line without restructuring — see architecture.md.
 */
@SpringBootApplication
public class CompaniesWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompaniesWatchApplication.class, args);
    }
}
