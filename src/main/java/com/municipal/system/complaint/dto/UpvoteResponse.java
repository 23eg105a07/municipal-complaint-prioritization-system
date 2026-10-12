package com.municipal.system.complaint.dto;

public record UpvoteResponse(
        Long complaintId,
        int affectedCount,
        boolean upvoted,
        String message
) {
}
