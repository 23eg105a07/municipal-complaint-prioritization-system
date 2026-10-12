package com.municipal.system.complaint.entity;

public enum ComplaintStatus {
    REPORTED,
    VERIFIED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REJECTED;

    public boolean isTerminal() {
        return this == CLOSED || this == REJECTED;
    }

    public boolean isResolvedOrClosed() {
        return this == RESOLVED || this == CLOSED;
    }
}
