package com.municipal.system.workflow.service;

import com.municipal.system.assignment.entity.Assignment;
import com.municipal.system.assignment.repository.AssignmentRepository;
import com.municipal.system.complaint.dto.ComplaintResponse;
import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.mapper.ComplaintMapper;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.exception.ForbiddenOperationException;
import com.municipal.system.exception.ResourceNotFoundException;
import com.municipal.system.security.UserPrincipal;
import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import com.municipal.system.workflow.dto.StatusLogResponse;
import com.municipal.system.workflow.dto.StatusUpdateRequest;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import com.municipal.system.workflow.repository.ComplaintStatusLogRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusLogRepository statusLogRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final WorkflowTransitionPolicy transitionPolicy;
    private final ComplaintMapper complaintMapper;
    private final Clock clock;

    @Transactional
    @CacheEvict(value = "priorityQueue", allEntries = true)
    public ComplaintResponse updateStatus(Long complaintId, StatusUpdateRequest request, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.status();

        transitionPolicy.validateTransition(oldStatus, newStatus);

        // Authorization validation
        if (currentUser.getRole() == Role.CITIZEN) {
            throw new ForbiddenOperationException("Citizens cannot manually update complaint status");
        }

        if (currentUser.getRole() == Role.OFFICER) {
            boolean matchesDept = currentUser.getDepartmentId() != null
                    && currentUser.getDepartmentId().equals(complaint.getDepartment().getId());
            boolean isAssigned = assignmentRepository.existsByComplaintIdAndOfficerIdAndActiveTrue(complaintId, currentUser.getId());

            if (!matchesDept && !isAssigned) {
                throw new ForbiddenOperationException("Field officer is not authorized to update complaints outside their assigned department");
            }
        }

        User changedBy = userRepository.findById(currentUser.getId()).orElse(null);

        complaint.setStatus(newStatus);
        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(clock.instant());
        } else if (oldStatus == ComplaintStatus.RESOLVED && newStatus != ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(null);
        }

        Complaint savedComplaint = complaintRepository.save(complaint);

        ComplaintStatusLog statusLog = ComplaintStatusLog.builder()
                .complaint(savedComplaint)
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .changedBy(changedBy)
                .remarks(request.remarks())
                .proofPhotoUrl(request.proofPhotoUrl())
                .build();
        statusLogRepository.save(statusLog);

        log.info("Complaint {} status transitioned: {} -> {} by user {}",
                complaintId, oldStatus, newStatus, currentUser.getEmail());

        Assignment activeAssignment = assignmentRepository.findByComplaintIdAndActiveTrue(complaintId).orElse(null);
        return complaintMapper.toResponse(savedComplaint, activeAssignment);
    }

    @Transactional(readOnly = true)
    public List<StatusLogResponse> getStatusHistory(Long complaintId) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint", "id", complaintId);
        }
        return statusLogRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(StatusLogResponse::fromEntity)
                .toList();
    }
}
