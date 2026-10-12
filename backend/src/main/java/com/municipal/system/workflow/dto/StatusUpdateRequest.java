package com.municipal.system.workflow.dto;

import com.municipal.system.complaint.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "New status is required")
        ComplaintStatus status,

        String remarks,

        String proofPhotoUrl
) {
}
