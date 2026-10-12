package com.municipal.system.workflow.dto;

import com.municipal.system.workflow.entity.CitizenFeedback;
import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Long complaintId,
        Long citizenId,
        String citizenName,
        Integer rating,
        String comments,
        Instant createdAt
) {
    public static FeedbackResponse fromEntity(CitizenFeedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getComplaint().getId(),
                feedback.getCitizen().getId(),
                feedback.getCitizen().getFullName(),
                feedback.getRating(),
                feedback.getComments(),
                feedback.getCreatedAt()
        );
    }
}
