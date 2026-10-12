package com.municipal.system.analytics.dto;

import com.municipal.system.complaint.entity.ComplaintStatus;
import java.util.List;

public record AnalyticsResponse(
        long totalComplaints,
        long pendingComplaints,
        long resolvedComplaints,
        long highPriorityComplaints,
        long escalatedComplaints,
        Double averageCitizenRating,
        List<ZoneMetric> complaintsByZone,
        List<DepartmentMetric> complaintsByDepartment,
        List<StatusMetric> complaintsByStatus
) {
    public record ZoneMetric(
            Integer zoneId,
            long count
    ) {
    }

    public record DepartmentMetric(
            Long departmentId,
            String departmentName,
            long count
    ) {
    }

    public record StatusMetric(
            ComplaintStatus status,
            long count
    ) {
    }
}
