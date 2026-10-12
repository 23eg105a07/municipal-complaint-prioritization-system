package com.municipal.system.prioritization.model;

import java.math.BigDecimal;

public record PriorityScoreBreakdown(
        BigDecimal finalScore,
        BigDecimal severityComponent,
        BigDecimal affectedComponent,
        BigDecimal ageHoursComponent,
        BigDecimal criticalHazardBoost,
        boolean isCriticalHazard,
        BigDecimal ageInHours,
        int affectedCount,
        int severity
) {
}
