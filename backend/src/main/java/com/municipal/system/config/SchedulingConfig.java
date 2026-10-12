package com.municipal.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled jobs. The scheduler pool size (2) is set in application.yml under
 * spring.task.scheduling.pool.size, so priority recalculation and SLA escalation run independently.
 * Each job is switched on or off by its own app.*.enabled property.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
