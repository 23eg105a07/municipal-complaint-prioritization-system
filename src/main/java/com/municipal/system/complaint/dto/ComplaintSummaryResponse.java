package com.municipal.system.complaint.dto;

import com.municipal.system.complaint.entity.ComplaintCategory;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.department.entity.DepartmentCode;
import java.math.BigDecimal;
import java.time.Instant;

public record ComplaintSummaryResponse(
        Long id,
        String title,
        ComplaintCategory category,
        DepartmentCode departmentCode,
        Integer severity,
        Integer affectedCount,
        BigDecimal priorityScore,
        ComplaintStatus status,
        Integer zoneId,
        Boolean isEscalated,
        Instant createdAt
) {
}
