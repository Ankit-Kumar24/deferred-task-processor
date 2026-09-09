package com.solidv.chronos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "chronos")
public record ChronosProperties(Scheduler scheduler, Retry retry) {

    public record Scheduler(
            int batchSize,
            Duration stuckTaskTimeout
    ) {
    }

    public record Retry(
            int maxRetries,
            long backoffBaseSeconds,
            int backoffMultiplier
    ) {
    }
}
