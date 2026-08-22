package com.flowset.admin.controller;

import com.flowset.admin.dto.AdminDashboardResponse;
import com.flowset.admin.service.AdminDashboardService;
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
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Dashboard", description = "ADMIN authorization required. Operational counts and date-filtered trends.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Platform dashboard", description = "range=TODAY|DAYS_7|DAYS_30|DAYS_90|CUSTOM")
    public AdminDashboardResponse dashboard(
            @RequestParam(defaultValue = "DAYS_30") String range,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return adminDashboardService.dashboard(range, from, to);
    }
}
