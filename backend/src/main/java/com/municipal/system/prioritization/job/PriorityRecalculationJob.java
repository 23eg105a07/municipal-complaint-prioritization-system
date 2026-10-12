package com.municipal.system.prioritization.job;

import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.config.AppProperties;
import com.municipal.system.prioritization.service.PriorityScoringService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriorityRecalculationJob {

    private final ComplaintRepository complaintRepository;
    private final PriorityScoringService priorityScoringService;
    private final AppProperties appProperties;

    private static final List<ComplaintStatus> TERMINAL_STATUSES = List.of(
            ComplaintStatus.RESOLVED,
            ComplaintStatus.CLOSED,
            ComplaintStatus.REJECTED
    );

    @Scheduled(
            initialDelayString = "${app.priority.recalculation.initial-delay-ms:60000}",
            fixedDelayString = "${app.priority.recalculation.fixed-delay-ms:300000}"
    )
    @Transactional
    @CacheEvict(value = "priorityQueue", allEntries = true)
    public void recalculateAllActivePriorities() {
        if (!appProperties.priority().recalculation().enabled()) {
            return;
        }

        log.info("Starting background priority score recalculation job...");
        List<Complaint> activeComplaints = complaintRepository.findByStatusNotIn(TERMINAL_STATUSES);
        int updatedCount = 0;

        for (Complaint complaint : activeComplaints) {
            BigDecimal newScore = priorityScoringService.calculateScore(complaint);
            if (complaint.getPriorityScore().compareTo(newScore) != 0) {
                complaint.setPriorityScore(newScore);
                complaintRepository.save(complaint);
                updatedCount++;
            }
        }

        log.info("Finished priority recalculation: checked {} complaints, updated {}.", activeComplaints.size(), updatedCount);
    }
}
