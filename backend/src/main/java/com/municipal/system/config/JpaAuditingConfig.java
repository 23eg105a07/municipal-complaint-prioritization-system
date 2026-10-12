package com.municipal.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing for automatic population of created_at and updated_at timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
