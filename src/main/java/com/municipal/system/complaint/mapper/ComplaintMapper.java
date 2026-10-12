package com.municipal.system.complaint.mapper;

import com.municipal.system.assignment.entity.Assignment;
import com.municipal.system.complaint.dto.ComplaintResponse;
import com.municipal.system.complaint.dto.ComplaintSummaryResponse;
import com.municipal.system.complaint.entity.Complaint;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public ComplaintResponse toResponse(Complaint complaint) {
        return toResponse(complaint, null);
    }

    public ComplaintResponse toResponse(Complaint complaint, Assignment activeAssignment) {
        Long assignedOfficerId = null;
        String assignedOfficerName = null;
        if (activeAssignment != null && activeAssignment.getOfficer() != null) {
            assignedOfficerId = activeAssignment.getOfficer().getId();
            assignedOfficerName = activeAssignment.getOfficer().getFullName();
        }

        return new ComplaintResponse(
                complaint.getId(),
                complaint.getTitle(),
                complaint.getDescription(),
                complaint.getCitizen().getId(),
                complaint.getCitizen().getFullName(),
                complaint.getDepartment().getId(),
                complaint.getDepartment().getCode(),
                complaint.getDepartment().getName(),
                complaint.getCategory(),
                complaint.getSeverity(),
                complaint.getAffectedCount(),
                complaint.getPriorityScore(),
                complaint.getStatus(),
                complaint.getZoneId(),
                complaint.getLatitude(),
                complaint.getLongitude(),
                complaint.getAddress(),
                complaint.getPhotoUrl(),
                complaint.getIsEscalated(),
                complaint.getEscalatedAt(),
                complaint.getResolvedAt(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                assignedOfficerId,
                assignedOfficerName
        );
    }

    public ComplaintSummaryResponse toSummaryResponse(Complaint complaint) {
        return new ComplaintSummaryResponse(
                complaint.getId(),
                complaint.getTitle(),
                complaint.getCategory(),
                complaint.getDepartment().getCode(),
                complaint.getSeverity(),
                complaint.getAffectedCount(),
                complaint.getPriorityScore(),
                complaint.getStatus(),
                complaint.getZoneId(),
                complaint.getIsEscalated(),
                complaint.getCreatedAt()
        );
    }
}
