package com.flowset.admin.controller;

import com.flowset.settings.dto.PlatformSettingsResponse;
import com.flowset.settings.dto.PlatformSettingsUpdateRequest;
import com.flowset.settings.service.PlatformSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@Tag(name = "Admin Settings", description = "ADMIN authorization required. Strongly typed platform settings.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final PlatformSettingsService platformSettingsService;

    public AdminSettingsController(PlatformSettingsService platformSettingsService) {
        this.platformSettingsService = platformSettingsService;
    }

    @GetMapping
    @Operation(summary = "Get platform settings")
    public PlatformSettingsResponse get() {
        return platformSettingsService.get();
    }

    @PutMapping
    @Operation(summary = "Update platform settings")
    public PlatformSettingsResponse update(@Valid @RequestBody PlatformSettingsUpdateRequest request) {
        return platformSettingsService.update(request);
    }
}
