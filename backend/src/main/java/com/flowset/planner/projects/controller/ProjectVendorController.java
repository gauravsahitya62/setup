package com.flowset.planner.projects.controller;

import com.flowset.planner.projects.dto.AddProjectVendorRequest;
import com.flowset.planner.projects.dto.ProjectVendorResponse;
import com.flowset.planner.projects.service.ProjectVendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/vendors")
@Tag(name = "Project Vendors", description = "Saved vendors. Unsaving does not cancel bookings.")
@SecurityRequirement(name = "bearerAuth")
public class ProjectVendorController {

    private final ProjectVendorService vendorService;

    public ProjectVendorController(ProjectVendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Save a vendor to the project")
    public ResponseEntity<ProjectVendorResponse> save(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectVendorRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.save(projectId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List saved vendors")
    public List<ProjectVendorResponse> list(@PathVariable UUID projectId) {
        return vendorService.list(projectId);
    }

    @DeleteMapping("/{vendorId}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Remove a saved vendor")
    public ResponseEntity<Void> remove(@PathVariable UUID projectId, @PathVariable UUID vendorId) {
        vendorService.remove(projectId, vendorId);
        return ResponseEntity.noContent().build();
    }
}
