package com.municipal.system.sla.service;

import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.config.AppProperties;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import com.municipal.system.workflow.repository.ComplaintStatusLogRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusLogRepository statusLogRepository;
    private final AppProperties appProperties;
    private final Clock clock;

    private static final List<ComplaintStatus> TERMINAL_STATUSES = List.of(
            ComplaintStatus.RESOLVED,
            ComplaintStatus.CLOSED,
            ComplaintStatus.REJECTED
    );

    public int getTargetHours(int severity) {
        return appProperties.sla().targetHoursFor(severity);
    }

    public Instant calculateDeadline(Complaint complaint) {
        if (complaint.getCreatedAt() == null) {
            return clock.instant();
        }
        int targetHours = getTargetHours(complaint.getSeverity());
        return complaint.getCreatedAt().plus(Duration.ofHours(targetHours));
    }

    public boolean isBreached(Complaint complaint) {
        if (complaint.getStatus().isResolvedOrClosed()) {
            return false;
        }
        Instant deadline = calculateDeadline(complaint);
        return clock.instant().isAfter(deadline);
    }

    @Transactional
    public int checkAndEscalateBreachedComplaints() {
        Instant now = clock.instant();
        List<Complaint> candidates = complaintRepository.findByStatusNotInAndIsEscalatedFalse(TERMINAL_STATUSES);
        int escalatedCount = 0;

        for (Complaint complaint : candidates) {
            int targetHours = getTargetHours(complaint.getSeverity());
            Instant deadline = complaint.getCreatedAt().plus(Duration.ofHours(targetHours));

            if (now.isAfter(deadline)) {
                complaint.setIsEscalated(Boolean.TRUE);
                complaint.setEscalatedAt(now);
                complaintRepository.save(complaint);

                ComplaintStatusLog logEntry = ComplaintStatusLog.builder()
                        .complaint(complaint)
                        .fromStatus(complaint.getStatus())
                        .toStatus(complaint.getStatus())
                        .changedBy(null) // Automated system action
                        .remarks(String.format("Automated SLA Escalation: Resolution deadline exceeded target of %d hours.", targetHours))
                        .build();
                statusLogRepository.save(logEntry);

                escalatedCount++;
                log.warn("Escalated complaint id: {} [Severity: {}, Target: {}h, Created: {}]",
                        complaint.getId(), complaint.getSeverity(), targetHours, complaint.getCreatedAt());
            }
        }

        return escalatedCount;
    }
}
