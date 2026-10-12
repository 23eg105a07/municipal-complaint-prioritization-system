package com.municipal.system.complaint.service;

import com.municipal.system.assignment.entity.Assignment;
import com.municipal.system.assignment.repository.AssignmentRepository;
import com.municipal.system.common.dto.PageResponse;
import com.municipal.system.complaint.dto.CreateComplaintRequest;
import com.municipal.system.complaint.dto.ComplaintResponse;
import com.municipal.system.complaint.dto.ComplaintSummaryResponse;
import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintCategory;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.mapper.ComplaintMapper;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.config.AppProperties;
import com.municipal.system.department.entity.Department;
import com.municipal.system.department.entity.DepartmentCode;
import com.municipal.system.department.repository.DepartmentRepository;
import com.municipal.system.exception.ResourceNotFoundException;
import com.municipal.system.prioritization.service.PriorityScoringService;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import com.municipal.system.workflow.repository.ComplaintStatusLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final ComplaintStatusLogRepository statusLogRepository;
    private final PriorityScoringService priorityScoringService;
    private final ComplaintMapper complaintMapper;
    private final AppProperties appProperties;
    private final Clock clock;

    private static final List<ComplaintStatus> TERMINAL_STATUSES = List.of(
            ComplaintStatus.RESOLVED,
            ComplaintStatus.CLOSED,
            ComplaintStatus.REJECTED
    );

    @Transactional
    @CacheEvict(value = "priorityQueue", allEntries = true)
    public ComplaintResponse createComplaint(CreateComplaintRequest request, Long citizenId) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", citizenId));

        DepartmentCode deptCode = request.category().getDepartmentCode();
        Department department = departmentRepository.findByCode(deptCode)
                .orElseThrow(() -> new ResourceNotFoundException("Department with code " + deptCode + " not found"));

        Instant now = clock.instant();
        int initialAffectedCount = appProperties.priority().baselineAffectedCount();
        BigDecimal initialScore = priorityScoringService.calculateScore(request.severity(), initialAffectedCount, now);

        Complaint complaint = Complaint.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .citizen(citizen)
                .department(department)
                .category(request.category())
                .severity(request.severity())
                .affectedCount(initialAffectedCount)
                .priorityScore(initialScore)
                .status(ComplaintStatus.REPORTED)
                .zoneId(request.zoneId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .address(request.address() != null ? request.address().trim() : null)
                .photoUrl(request.photoUrl() != null ? request.photoUrl().trim() : null)
                .isEscalated(Boolean.FALSE)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // Record initial status log
        ComplaintStatusLog logEntry = ComplaintStatusLog.builder()
                .complaint(saved)
                .fromStatus(null)
                .toStatus(ComplaintStatus.REPORTED)
                .changedBy(citizen)
                .remarks("Complaint submitted by citizen")
                .proofPhotoUrl(saved.getPhotoUrl())
                .build();
        statusLogRepository.save(logEntry);

        log.info("Created complaint id: {} [Category: {}, Dept: {}, Severity: {}, Score: {}]",
                saved.getId(), saved.getCategory(), department.getCode(), saved.getSeverity(), saved.getPriorityScore());

        return complaintMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));
        Assignment activeAssignment = assignmentRepository.findByComplaintIdAndActiveTrue(id).orElse(null);
        return complaintMapper.toResponse(complaint, activeAssignment);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "priorityQueue", key = "'pq::page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public PageResponse<ComplaintSummaryResponse> getPriorityQueue(Pageable pageable) {
        Page<Complaint> complaints = complaintRepository.findActivePriorityQueue(TERMINAL_STATUSES, pageable);
        return PageResponse.fromPage(complaints.map(complaintMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintSummaryResponse> getComplaints(
            ComplaintStatus status,
            Long departmentId,
            Integer zoneId,
            Long citizenId,
            Pageable pageable) {

        Page<Complaint> page;
        if (citizenId != null) {
            page = complaintRepository.findByCitizenId(citizenId, pageable);
        } else if (departmentId != null) {
            page = complaintRepository.findByDepartmentId(departmentId, pageable);
        } else if (zoneId != null) {
            page = complaintRepository.findByZoneId(zoneId, pageable);
        } else if (status != null) {
            page = complaintRepository.findByStatus(status, pageable);
        } else {
            page = complaintRepository.findAll(pageable);
        }

        return PageResponse.fromPage(page.map(complaintMapper::toSummaryResponse));
    }
}
