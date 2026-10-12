package com.municipal.system.sla.job;

import com.municipal.system.config.AppProperties;
import com.municipal.system.sla.service.SlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaEscalationJob {

    private final SlaService slaService;
    private final AppProperties appProperties;

    @Scheduled(
            initialDelayString = "${app.sla.escalation.initial-delay-ms:90000}",
            fixedDelayString = "${app.sla.escalation.fixed-delay-ms:600000}"
    )
    public void executeSlaCheck() {
        if (!appProperties.sla().escalation().enabled()) {
            return;
        }

        log.info("Starting background SLA breach and escalation check job...");
        int escalated = slaService.checkAndEscalateBreachedComplaints();
        log.info("Finished SLA escalation check: escalated {} complaints.", escalated);
    }
}
