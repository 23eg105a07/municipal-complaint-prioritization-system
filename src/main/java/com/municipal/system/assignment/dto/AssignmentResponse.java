package com.municipal.system.assignment.dto;

import com.municipal.system.assignment.entity.Assignment;
import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long complaintId,
        Long officerId,
        String officerName,
        Long assignedById,
        String assignedByName,
        Instant assignedAt,
        String notes,
        boolean active
) {
    public static AssignmentResponse fromEntity(Assignment assignment) {
        Long assignedById = assignment.getAssignedBy() != null ? assignment.getAssignedBy().getId() : null;
        String assignedByName = assignment.getAssignedBy() != null ? assignment.getAssignedBy().getFullName() : null;
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getComplaint().getId(),
                assignment.getOfficer().getId(),
                assignment.getOfficer().getFullName(),
                assignedById,
                assignedByName,
                assignment.getAssignedAt(),
                assignment.getNotes(),
                assignment.getActive()
        );
    }
}
