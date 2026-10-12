package com.municipal.system.workflow.service;

import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.exception.BusinessRuleException;
import com.municipal.system.exception.ConflictException;
import com.municipal.system.exception.ForbiddenOperationException;
import com.municipal.system.exception.ResourceNotFoundException;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import com.municipal.system.workflow.dto.FeedbackRequest;
import com.municipal.system.workflow.dto.FeedbackResponse;
import com.municipal.system.workflow.entity.CitizenFeedback;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import com.municipal.system.workflow.repository.CitizenFeedbackRepository;
import com.municipal.system.workflow.repository.ComplaintStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final ComplaintRepository complaintRepository;
    private final CitizenFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ComplaintStatusLogRepository statusLogRepository;

    @Transactional
    public FeedbackResponse submitFeedback(Long complaintId, FeedbackRequest request, Long citizenId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new ForbiddenOperationException("Only the citizen who filed this complaint can submit feedback");
        }

        if (!complaint.getStatus().isResolvedOrClosed()) {
            throw new BusinessRuleException("Feedback can only be submitted after the complaint is resolved");
        }

        if (feedbackRepository.existsByComplaintId(complaintId)) {
            throw new ConflictException("Feedback has already been submitted for this complaint");
        }

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", citizenId));

        CitizenFeedback feedback = CitizenFeedback.builder()
                .complaint(complaint)
                .citizen(citizen)
                .rating(request.rating())
                .comments(request.comments() != null ? request.comments().trim() : null)
                .build();
        CitizenFeedback saved = feedbackRepository.save(feedback);

        // Auto-close ticket upon citizen feedback
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setStatus(ComplaintStatus.CLOSED);
            complaintRepository.save(complaint);

            ComplaintStatusLog logEntry = ComplaintStatusLog.builder()
                    .complaint(complaint)
                    .fromStatus(ComplaintStatus.RESOLVED)
                    .toStatus(ComplaintStatus.CLOSED)
                    .changedBy(citizen)
                    .remarks(String.format("Citizen submitted feedback (rating: %d/5). Ticket closed.", request.rating()))
                    .build();
            statusLogRepository.save(logEntry);
        }

        log.info("Citizen {} submitted feedback for complaint {}. Rating: {}", citizenId, complaintId, request.rating());
        return FeedbackResponse.fromEntity(saved);
    }
}
