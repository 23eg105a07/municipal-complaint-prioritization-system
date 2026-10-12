package com.municipal.system.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Typed, validated binding for every {@code app.*} property in application.yml.
 * The application fails at startup with a clear message if any value is invalid.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Security security,
        @Valid @NotNull Priority priority,
        @Valid @NotNull Sla sla,
        @Valid @NotNull Analytics analytics,
        @Valid @NotNull Cache cache,
        @Valid @DefaultValue Cors cors,
        @Valid @DefaultValue BootstrapAdmin bootstrapAdmin) {

    // ----------------------------------------------------------------- security

    public record Security(@Valid @NotNull Jwt jwt) {
    }

    public record Jwt(
            @NotBlank
            @Size(min = 32, message = "JWT secret must be at least 32 characters long")
            String secret,
            @Min(1) long expirationMinutes,
            @NotBlank String issuer) {
    }

    // ---------------------------------------------------------------- priority

    /**
     * Priority Score = (severity * weights.severity) + (affectedPeople * weights.affectedPeople)
     * + (ageInHours * weights.ageHours), plus criticalHazardBoost when
     * severity >= criticalSeverityThreshold. The result is rounded once, to scoreScale
     * decimal places, using roundingMode.
     */
    public record Priority(
            @Valid @NotNull Weights weights,
            @NotNull @DecimalMin("0.0") BigDecimal criticalHazardBoost,
            @Min(1) @Max(5) int criticalSeverityThreshold,
            @Min(0) int baselineAffectedCount,
            @Min(0) @Max(6) int scoreScale,
            @NotNull RoundingMode roundingMode,
            @Valid @NotNull Recalculation recalculation) {
    }

    public record Weights(
            @NotNull @DecimalMin("0.0") BigDecimal severity,
            @NotNull @DecimalMin("0.0") BigDecimal affectedPeople,
            @NotNull @DecimalMin("0.0") BigDecimal ageHours) {
    }

    public record Recalculation(
            boolean enabled,
            @Min(1000) long initialDelayMs,
            @Min(1000) long fixedDelayMs) {
    }

    // ---------------------------------------------------------------------- sla

    public record Sla(
            @NotEmpty Map<Integer, @Positive Integer> targetHoursBySeverity,
            @Valid @NotNull Escalation escalation) {

        /** Resolution target in hours for the given severity (1-5). */
        public int targetHoursFor(int severity) {
            Integer hours = targetHoursBySeverity.get(severity);
            if (hours == null) {
                throw new IllegalStateException("No SLA target configured for severity " + severity);
            }
            return hours;
        }

        @AssertTrue(message = "app.sla.target-hours-by-severity must define a target for every severity from 1 to 5")
        public boolean isEverySeverityConfigured() {
            if (targetHoursBySeverity == null) {
                return true; // reported by @NotEmpty
            }
            for (int severity = 1; severity <= 5; severity++) {
                if (!targetHoursBySeverity.containsKey(severity)) {
                    return false;
                }
            }
            return true;
        }
    }

    public record Escalation(
            boolean enabled,
            @Min(1000) long initialDelayMs,
            @Min(1000) long fixedDelayMs) {
    }

    // ---------------------------------------------------------------- analytics

    public record Analytics(@NotNull @DecimalMin("0.0") BigDecimal highPriorityThreshold) {
    }

    // -------------------------------------------------------------------- cache

    public record Cache(
            @NotNull Duration analyticsTtl,
            @NotNull Duration priorityQueueTtl) {
    }

    // --------------------------------------------------------------------- cors

    public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {
    }

    // --------------------------------------------------------- bootstrap admin

    public record BootstrapAdmin(
            @DefaultValue("System Administrator") String fullName,
            @DefaultValue("") String email,
            @DefaultValue("") String password) {

        /** True only when both an email and a password were supplied. */
        public boolean configured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
