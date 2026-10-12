package com.municipal.system.workflow.service;

import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.exception.BusinessRuleException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WorkflowTransitionPolicy {

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ComplaintStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ComplaintStatus.REPORTED, Set.of(
                ComplaintStatus.VERIFIED,
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.REJECTED
        ));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.VERIFIED, Set.of(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.REJECTED
        ));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.ASSIGNED, Set.of(
                ComplaintStatus.IN_PROGRESS,
                ComplaintStatus.RESOLVED,
                ComplaintStatus.REJECTED
        ));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.IN_PROGRESS, Set.of(
                ComplaintStatus.RESOLVED,
                ComplaintStatus.REJECTED
        ));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.RESOLVED, Set.of(
                ComplaintStatus.CLOSED,
                ComplaintStatus.IN_PROGRESS
        ));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.REJECTED, Set.of(
                ComplaintStatus.REPORTED
        ));
        ALLOWED_TRANSITIONS.put(ComplaintStatus.CLOSED, Collections.emptySet());
    }

    public boolean isValidTransition(ComplaintStatus from, ComplaintStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return false;
        }
        Set<ComplaintStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        return allowed.contains(to);
    }

    public void validateTransition(ComplaintStatus from, ComplaintStatus to) {
        if (!isValidTransition(from, to)) {
            throw new BusinessRuleException(String.format("Invalid status transition from '%s' to '%s'", from, to));
        }
    }
}
