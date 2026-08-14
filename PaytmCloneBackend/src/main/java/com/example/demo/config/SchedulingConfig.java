package com.example.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled jobs only for the active application slot.
 *
 * <p>Azure App Service deployment slots are live applications. Without this
 * guard, both production and staging execute settlement and cleanup jobs
 * against the production database. Terraform keeps this setting enabled on the
 * production slot and disabled as a sticky setting on staging.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "app.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SchedulingConfig {
}
