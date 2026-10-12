package com.municipal.system.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single injectable UTC clock. Services use it instead of calling Instant.now() directly,
 * so tests can supply a fixed clock and stay deterministic.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
