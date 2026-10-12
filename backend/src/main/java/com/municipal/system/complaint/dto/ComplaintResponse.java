package com.municipal.system.complaint.dto;

import com.municipal.system.complaint.entity.ComplaintCategory;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.department.entity.DepartmentCode;
import java.math.BigDecimal;
import java.time.Instant;

public record ComplaintResponse(
        Long id,
        String title,
        String description,
        Long citizenId,
        String citizenName,
        Long departmentId,
        DepartmentCode departmentCode,
        String departmentName,
        ComplaintCategory category,
        Integer severity,
        Integer affectedCount,
        BigDecimal priorityScore,
        ComplaintStatus status,
        Integer zoneId,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String photoUrl,
        Boolean isEscalated,
        Instant escalatedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt,
        Long assignedOfficerId,
        String assignedOfficerName
) {
}
