package com.municipal.system.prioritization.service;

import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.config.AppProperties;
import com.municipal.system.prioritization.model.PriorityScoreBreakdown;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriorityScoringService {

    private final AppProperties appProperties;
    private final Clock clock;

    public BigDecimal calculateScore(Complaint complaint) {
        return calculateBreakdown(complaint.getSeverity(), complaint.getAffectedCount(), complaint.getCreatedAt(), clock.instant()).finalScore();
    }

    public BigDecimal calculateScore(int severity, int affectedCount, Instant createdAt) {
        return calculateBreakdown(severity, affectedCount, createdAt, clock.instant()).finalScore();
    }

    public PriorityScoreBreakdown calculateBreakdown(int severity, int affectedCount, Instant createdAt) {
        return calculateBreakdown(severity, affectedCount, createdAt, clock.instant());
    }

    public PriorityScoreBreakdown calculateBreakdown(int severity, int affectedCount, Instant createdAt, Instant evaluationTime) {
        AppProperties.Priority priorityConfig = appProperties.priority();
        AppProperties.Weights weights = priorityConfig.weights();

        long elapsedSeconds = 0;
        if (createdAt != null && evaluationTime != null) {
            elapsedSeconds = Math.max(0, Duration.between(createdAt, evaluationTime).getSeconds());
        }

        BigDecimal ageInHours = BigDecimal.valueOf(elapsedSeconds)
                .divide(BigDecimal.valueOf(3600), 8, RoundingMode.HALF_UP);

        BigDecimal severityComponent = BigDecimal.valueOf(severity).multiply(weights.severity());
        BigDecimal affectedComponent = BigDecimal.valueOf(affectedCount).multiply(weights.affectedPeople());
        BigDecimal ageHoursComponent = ageInHours.multiply(weights.ageHours());

        boolean isCriticalHazard = severity >= priorityConfig.criticalSeverityThreshold();
        BigDecimal boost = isCriticalHazard ? priorityConfig.criticalHazardBoost() : BigDecimal.ZERO;

        BigDecimal rawScore = severityComponent
                .add(affectedComponent)
                .add(ageHoursComponent)
                .add(boost);

        BigDecimal finalScore = rawScore.setScale(priorityConfig.scoreScale(), priorityConfig.roundingMode());

        return new PriorityScoreBreakdown(
                finalScore,
                severityComponent.setScale(priorityConfig.scoreScale(), priorityConfig.roundingMode()),
                affectedComponent.setScale(priorityConfig.scoreScale(), priorityConfig.roundingMode()),
                ageHoursComponent.setScale(priorityConfig.scoreScale(), priorityConfig.roundingMode()),
                boost.setScale(priorityConfig.scoreScale(), priorityConfig.roundingMode()),
                isCriticalHazard,
                ageInHours.setScale(2, RoundingMode.HALF_UP),
                affectedCount,
                severity
        );
    }
}
