package com.municipal.system.complaint.controller;

import com.municipal.system.assignment.dto.AssignOfficerRequest;
import com.municipal.system.assignment.dto.AssignmentResponse;
import com.municipal.system.assignment.service.AssignmentService;
import com.municipal.system.common.dto.PageResponse;
import com.municipal.system.complaint.dto.ComplaintResponse;
import com.municipal.system.complaint.dto.ComplaintSummaryResponse;
import com.municipal.system.complaint.dto.CreateComplaintRequest;
import com.municipal.system.complaint.dto.UpvoteResponse;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.service.ComplaintService;
import com.municipal.system.complaint.service.UpvoteService;
import com.municipal.system.security.UserPrincipal;
import com.municipal.system.workflow.dto.FeedbackRequest;
import com.municipal.system.workflow.dto.FeedbackResponse;
import com.municipal.system.workflow.dto.StatusLogResponse;
import com.municipal.system.workflow.dto.StatusUpdateRequest;
import com.municipal.system.workflow.service.FeedbackService;
import com.municipal.system.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Complaint lifecycle, assignments, status transitions, and citizen feedback")
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UpvoteService upvoteService;
    private final AssignmentService assignmentService;
    private final WorkflowService workflowService;
    private final FeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "Submit complaint", description = "Citizen submits a new municipal complaint with geo-location and severity.")
    public ResponseEntity<ComplaintResponse> submitComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintResponse response = complaintService.createComplaint(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/priority")
    @Operation(summary = "Fetch priority queue", description = "Retrieves active complaints dynamically ordered by priority score descending.")
    public ResponseEntity<PageResponse<ComplaintSummaryResponse>> getPriorityQueue(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ComplaintSummaryResponse> queue = complaintService.getPriorityQueue(pageable);
        return ResponseEntity.ok(queue);
    }

    @GetMapping
    @Operation(summary = "List complaints", description = "Lists complaints with optional filtering by status, department, zone, or citizen.")
    public ResponseEntity<PageResponse<ComplaintSummaryResponse>> listComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) Long citizenId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ComplaintSummaryResponse> response = complaintService.getComplaints(
                status, departmentId, zoneId, citizenId, pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complaint details", description = "Fetches complete details for a complaint including department and assigned officer.")
    public ResponseEntity<ComplaintResponse> getComplaint(@PathVariable Long id) {
        ComplaintResponse response = complaintService.getComplaintById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/upvote")
    @Operation(summary = "Upvote complaint", description = "Citizens upvote existing local complaints, increasing affected count and recalculating priority.")
    public ResponseEntity<UpvoteResponse> upvoteComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UpvoteResponse response = upvoteService.upvoteComplaint(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign to officer", description = "Admin assigns a complaint to a field officer matching zone and department.")
    public ResponseEntity<AssignmentResponse> assignOfficer(
            @PathVariable Long id,
            @Valid @RequestBody AssignOfficerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AssignmentResponse response = assignmentService.assignOfficer(id, request, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
    @Operation(summary = "Update complaint status stage", description = "Officers or admins advance complaint stages with proof of work photo and remarks.")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ComplaintResponse response = workflowService.updateStatus(id, request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get complaint audit trail", description = "Retrieves timeline of status transitions and officer remarks.")
    public ResponseEntity<List<StatusLogResponse>> getComplaintHistory(@PathVariable Long id) {
        List<StatusLogResponse> history = workflowService.getStatusHistory(id);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Submit citizen resolution feedback", description = "Reporting citizen reviews the resolution rating 1-5 and signs off.")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        FeedbackResponse response = feedbackService.submitFeedback(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
