package com.municipal.system.workflow.dto;

import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import java.time.Instant;

public record StatusLogResponse(
        Long id,
        Long complaintId,
        ComplaintStatus fromStatus,
        ComplaintStatus toStatus,
        Long changedById,
        String changedByName,
        String remarks,
        String proofPhotoUrl,
        Instant createdAt
) {
    public static StatusLogResponse fromEntity(ComplaintStatusLog log) {
        Long userId = log.getChangedBy() != null ? log.getChangedBy().getId() : null;
        String userName = log.getChangedBy() != null ? log.getChangedBy().getFullName() : null;
        return new StatusLogResponse(
                log.getId(),
                log.getComplaint().getId(),
                log.getFromStatus(),
                log.getToStatus(),
                userId,
                userName,
                log.getRemarks(),
                log.getProofPhotoUrl(),
                log.getCreatedAt()
        );
    }
}
