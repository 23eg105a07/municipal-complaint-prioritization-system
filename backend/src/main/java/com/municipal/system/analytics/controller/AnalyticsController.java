package com.municipal.system.analytics.controller;

import com.municipal.system.analytics.dto.AnalyticsResponse;
import com.municipal.system.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/reports", "/api/analytics"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OFFICER')")
@Tag(name = "Analytics & Reports", description = "Municipal turnaround statistics, heatmaps, and zone metrics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/analytics")
    @Operation(summary = "Get municipal analytics summary", description = "Aggregates complaint totals, department turnaround counts, and zone statistics.")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        AnalyticsResponse response = analyticsService.getAnalyticsSummary();
        return ResponseEntity.ok(response);
    }
}
