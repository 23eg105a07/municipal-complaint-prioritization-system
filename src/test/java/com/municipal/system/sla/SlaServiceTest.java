package com.municipal.system.sla;

import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.config.AppProperties;
import com.municipal.system.sla.service.SlaService;
import com.municipal.system.workflow.entity.ComplaintStatusLog;
import com.municipal.system.workflow.repository.ComplaintStatusLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintStatusLogRepository statusLogRepository;

    private SlaService slaService;
    private Clock fixedClock;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-01-05T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        appProperties = new AppProperties(
                new AppProperties.Security(new AppProperties.Jwt("dummy-secret-32-chars-long-minimum!", 60, "test")),
                new AppProperties.Priority(
                        new AppProperties.Weights(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
                        BigDecimal.TEN, 5, 1, 2, RoundingMode.HALF_UP,
                        new AppProperties.Recalculation(true, 1000, 5000)
                ),
                new AppProperties.Sla(
                        Map.of(1, 168, 2, 120, 3, 72, 4, 48, 5, 24),
                        new AppProperties.Escalation(true, 1000, 5000)
                ),
                new AppProperties.Analytics(new BigDecimal("25.0")),
                new AppProperties.Cache(Duration.ofMinutes(5), Duration.ofMinutes(1)),
                new AppProperties.Cors(null),
                new AppProperties.BootstrapAdmin("Admin", "admin@municipal.gov", "Admin@123")
        );

        slaService = new SlaService(complaintRepository, statusLogRepository, appProperties, fixedClock);
    }

    @Test
    @DisplayName("SLA target hours mapped accurately by severity level")
    void testTargetHours() {
        assertThat(slaService.getTargetHours(1)).isEqualTo(168);
        assertThat(slaService.getTargetHours(2)).isEqualTo(120);
        assertThat(slaService.getTargetHours(3)).isEqualTo(72);
        assertThat(slaService.getTargetHours(4)).isEqualTo(48);
        assertThat(slaService.getTargetHours(5)).isEqualTo(24);
    }

    @Test
    @DisplayName("Detect breached complaint when resolution time exceeded")
    void testBreachDetection() {
        // Severity 5 has target of 24h
        // Created 25 hours ago -> breached!
        Instant createdAt = fixedClock.instant().minus(Duration.ofHours(25));
        Complaint complaint = Complaint.builder()
                .id(1L)
                .severity(5)
                .status(ComplaintStatus.IN_PROGRESS)
                .isEscalated(false)
                .build();
        complaint.setCreatedAt(createdAt);

        assertThat(slaService.isBreached(complaint)).isTrue();
    }

    @Test
    @DisplayName("Check and escalate sets flag, timestamp, and writes audit log")
    void testCheckAndEscalateBreachedComplaints() {
        Instant createdAt = fixedClock.instant().minus(Duration.ofHours(30));
        Complaint complaint = Complaint.builder()
                .id(1L)
                .severity(5)
                .status(ComplaintStatus.IN_PROGRESS)
                .isEscalated(false)
                .build();
        complaint.setCreatedAt(createdAt);

        when(complaintRepository.findByStatusNotInAndIsEscalatedFalse(any())).thenReturn(List.of(complaint));

        int count = slaService.checkAndEscalateBreachedComplaints();

        assertThat(count).isEqualTo(1);
        assertThat(complaint.getIsEscalated()).isTrue();
        assertThat(complaint.getEscalatedAt()).isEqualTo(fixedClock.instant());
        verify(complaintRepository).save(complaint);
        verify(statusLogRepository).save(any(ComplaintStatusLog.class));
    }
}
