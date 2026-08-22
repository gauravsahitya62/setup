package com.flowset.admin.controller;

import com.flowset.admin.dto.AdminReportsResponse;
import com.flowset.admin.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(
        name = "Admin Reports",
        description = "ADMIN authorization required. Financial figures are Estimated Rental Value / Gross Booking Value. FlowSet does not collect payments in MVP."
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    @Operation(summary = "MVP operational reports")
    public AdminReportsResponse reports(
            @RequestParam(defaultValue = "DAYS_30") String range,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return adminReportService.reports(range, from, to);
    }
}
