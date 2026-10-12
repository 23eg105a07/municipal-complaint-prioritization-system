package com.municipal.system.prioritization;

import com.municipal.system.config.AppProperties;
import com.municipal.system.prioritization.model.PriorityScoreBreakdown;
import com.municipal.system.prioritization.service.PriorityScoringService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityScoringServiceTest {

    private PriorityScoringService scoringService;
    private Clock fixedClock;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-01-01T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        appProperties = new AppProperties(
                new AppProperties.Security(new AppProperties.Jwt("dummy-secret-32-chars-long-minimum!", 60, "test")),
                new AppProperties.Priority(
                        new AppProperties.Weights(new BigDecimal("0.5"), new BigDecimal("0.3"), new BigDecimal("0.2")),
                        new BigDecimal("50.0"),
                        5,
                        1,
                        2,
                        RoundingMode.HALF_UP,
                        new AppProperties.Recalculation(true, 1000, 5000)
                ),
                new AppProperties.Sla(
                        Map.of(1, 168, 2, 120, 3, 72, 4, 48, 5, 24),
                        new AppProperties.Escalation(true, 1000, 5000)
                ),
                new AppProperties.Analytics(new BigDecimal("25.0")),
                new AppProperties.Cache(java.time.Duration.ofMinutes(5), java.time.Duration.ofMinutes(1)),
                new AppProperties.Cors(null),
                new AppProperties.BootstrapAdmin("Admin", "admin@municipal.gov", "Admin@123")
        );

        scoringService = new PriorityScoringService(appProperties, fixedClock);
    }

    @Test
    @DisplayName("Calculate score without critical boost when severity < 5")
    void testScoreWithoutCriticalBoost() {
        // severity = 3, affected = 5, age = 2 hours
        // Severity component: 3 * 0.5 = 1.5
        // Affected component: 5 * 0.3 = 1.5
        // Age component: 2 * 0.2 = 0.4
        // Total = 1.5 + 1.5 + 0.4 = 3.40
        Instant createdAt = fixedClock.instant().minusSeconds(7200); // 2 hours ago

        PriorityScoreBreakdown breakdown = scoringService.calculateBreakdown(3, 5, createdAt);

        assertThat(breakdown.isCriticalHazard()).isFalse();
        assertThat(breakdown.criticalHazardBoost()).isEqualByComparingTo("0.00");
        assertThat(breakdown.severityComponent()).isEqualByComparingTo("1.50");
        assertThat(breakdown.affectedComponent()).isEqualByComparingTo("1.50");
        assertThat(breakdown.ageHoursComponent()).isEqualByComparingTo("0.40");
        assertThat(breakdown.finalScore()).isEqualByComparingTo("3.40");
    }

    @Test
    @DisplayName("Calculate score with critical hazard boost (+50) when severity = 5")
    void testScoreWithCriticalHazardBoost() {
        // severity = 5, affected = 10, age = 1 hour
        // Severity component: 5 * 0.5 = 2.5
        // Affected component: 10 * 0.3 = 3.0
        // Age component: 1 * 0.2 = 0.2
        // Critical boost: 50.0
        // Total = 2.5 + 3.0 + 0.2 + 50.0 = 55.70
        Instant createdAt = fixedClock.instant().minusSeconds(3600); // 1 hour ago

        PriorityScoreBreakdown breakdown = scoringService.calculateBreakdown(5, 10, createdAt);

        assertThat(breakdown.isCriticalHazard()).isTrue();
        assertThat(breakdown.criticalHazardBoost()).isEqualByComparingTo("50.00");
        assertThat(breakdown.finalScore()).isEqualByComparingTo("55.70");
    }

    @Test
    @DisplayName("Fractional hours and rounding HALF_UP to 2 decimal places")
    void testScoreFractionalHoursAndRounding() {
        // severity = 2, affected = 1, age = 45 minutes (0.75 hours)
        // Severity: 2 * 0.5 = 1.0
        // Affected: 1 * 0.3 = 0.3
        // Age: 0.75 * 0.2 = 0.15
        // Total = 1.0 + 0.3 + 0.15 = 1.45
        Instant createdAt = fixedClock.instant().minusSeconds(2700); // 45 mins

        BigDecimal score = scoringService.calculateScore(2, 1, createdAt);

        assertThat(score).isEqualByComparingTo("1.45");
    }
}
