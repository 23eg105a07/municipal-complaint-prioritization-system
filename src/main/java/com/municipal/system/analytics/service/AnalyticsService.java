package com.municipal.system.analytics.service;

import com.municipal.system.analytics.dto.AnalyticsResponse;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.config.AppProperties;
import com.municipal.system.workflow.repository.CitizenFeedbackRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;
    private final CitizenFeedbackRepository feedbackRepository;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'summary'")
    public AnalyticsResponse getAnalyticsSummary() {
        log.info("Calculating live municipal complaint analytics metrics...");

        long totalComplaints = complaintRepository.count();
        long resolved = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long closed = complaintRepository.countByStatus(ComplaintStatus.CLOSED);
        long rejected = complaintRepository.countByStatus(ComplaintStatus.REJECTED);
        long resolvedComplaints = resolved + closed;
        long pendingComplaints = Math.max(0, totalComplaints - resolvedComplaints - rejected);

        long highPriority = complaintRepository.countByPriorityScoreGreaterThanEqual(
                appProperties.analytics().highPriorityThreshold()
        );
        long escalated = complaintRepository.countByIsEscalatedTrue();

        Double avgRating = feedbackRepository.findAverageRating();

        // Zone breakdown
        List<AnalyticsResponse.ZoneMetric> zoneMetrics = new ArrayList<>();
        List<Object[]> zoneCounts = complaintRepository.countComplaintsGroupByZone();
        for (Object[] row : zoneCounts) {
            Integer zoneId = (Integer) row[0];
            long count = ((Number) row[1]).longValue();
            zoneMetrics.add(new AnalyticsResponse.ZoneMetric(zoneId, count));
        }

        // Department breakdown
        List<AnalyticsResponse.DepartmentMetric> deptMetrics = new ArrayList<>();
        List<Object[]> deptCounts = complaintRepository.countComplaintsGroupByDepartment();
        for (Object[] row : deptCounts) {
            Long deptId = ((Number) row[0]).longValue();
            String deptName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            deptMetrics.add(new AnalyticsResponse.DepartmentMetric(deptId, deptName, count));
        }

        // Status breakdown
        List<AnalyticsResponse.StatusMetric> statusMetrics = new ArrayList<>();
        List<Object[]> statusCounts = complaintRepository.countComplaintsGroupByStatus();
        for (Object[] row : statusCounts) {
            ComplaintStatus status = (ComplaintStatus) row[0];
            long count = ((Number) row[1]).longValue();
            statusMetrics.add(new AnalyticsResponse.StatusMetric(status, count));
        }

        return new AnalyticsResponse(
                totalComplaints,
                pendingComplaints,
                resolvedComplaints,
                highPriority,
                escalated,
                avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : null,
                zoneMetrics,
                deptMetrics,
                statusMetrics
        );
    }
}
