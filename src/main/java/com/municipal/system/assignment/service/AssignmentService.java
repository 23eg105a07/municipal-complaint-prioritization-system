package com.municipal.system.assignment.service;

import com.municipal.system.assignment.dto.AssignOfficerRequest;
import com.municipal.system.assignment.dto.AssignmentResponse;
import com.municipal.system.assignment.entity.Assignment;
import com.municipal.system.assignment.repository.AssignmentRepository;
import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.exception.BusinessRuleException;
import com.municipal.system.exception.ResourceNotFoundException;
import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import com.municipal.system.workflow.repository.ComplaintStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintStatusLogRepository statusLogRepository;

    @Transactional
    @CacheEvict(value = "priorityQueue", allEntries = true)
    public AssignmentResponse assignOfficer(Long complaintId, AssignOfficerRequest request, Long assignedById) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        if (complaint.getStatus().isTerminal()) {
            throw new BusinessRuleException("Cannot assign officer to a closed or rejected complaint");
        }

        User officer = userRepository.findById(request.officerId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer", "id", request.officerId()));

        if (officer.getRole() != Role.OFFICER) {
            throw new BusinessRuleException("Assigned user must hold the OFFICER role");
        }

        if (officer.getDepartment() == null || !officer.getDepartment().getId().equals(complaint.getDepartment().getId())) {
            throw new BusinessRuleException(String.format(
                    "Officer belongs to department '%s' but complaint is for department '%s'",
                    officer.getDepartment() != null ? officer.getDepartment().getCode() : "None",
                    complaint.getDepartment().getCode()
            ));
        }

        if (Boolean.FALSE.equals(officer.getAvailable())) {
            throw new BusinessRuleException("Officer is currently marked as unavailable");
        }

        User assigner = assignedById != null ? userRepository.findById(assignedById).orElse(null) : null;

        // Deactivate previous active assignment
        assignmentRepository.findByComplaintIdAndActiveTrue(complaintId).ifPresent(prev -> {
            prev.setActive(Boolean.FALSE);
            assignmentRepository.save(prev);
        });

        Assignment assignment = Assignment.builder()
                .complaint(complaint)
                .officer(officer)
                .assignedBy(assigner)
                .notes(request.notes())
                .active(Boolean.TRUE)
                .build();
        Assignment savedAssignment = assignmentRepository.save(assignment);

        // Update complaint status to ASSIGNED if currently REPORTED or VERIFIED
        ComplaintStatus oldStatus = complaint.getStatus();
        if (oldStatus == ComplaintStatus.REPORTED || oldStatus == ComplaintStatus.VERIFIED) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
            complaintRepository.save(complaint);

            ComplaintStatusLog logEntry = ComplaintStatusLog.builder()
                    .complaint(complaint)
                    .fromStatus(oldStatus)
                    .toStatus(ComplaintStatus.ASSIGNED)
                    .changedBy(assigner != null ? assigner : officer)
                    .remarks("Assigned to field officer: " + officer.getFullName())
                    .build();
            statusLogRepository.save(logEntry);
        }

        log.info("Assigned complaint {} to officer {} by user {}", complaintId, officer.getId(), assignedById);
        return AssignmentResponse.fromEntity(savedAssignment);
    }
}
