package com.companieswatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async} so alert dispatch runs off the streaming/web threads. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
