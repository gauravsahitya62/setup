package com.flowset.planner.projects.controller;

import com.flowset.planner.projects.dto.AddProjectProductRequest;
import com.flowset.planner.projects.dto.ProjectProductResponse;
import com.flowset.planner.projects.dto.UpdateProjectProductRequest;
import com.flowset.planner.projects.service.ProjectProductService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/products")
@Tag(name = "Project Products", description = "Shortlist marketplace variants. Does not reserve inventory.")
@SecurityRequirement(name = "bearerAuth")
public class ProjectProductController {

    private final ProjectProductService productService;

    public ProjectProductController(ProjectProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Add a product variant to the project")
    public ResponseEntity<ProjectProductResponse> add(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.add(projectId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List products in the project")
    public List<ProjectProductResponse> list(@PathVariable UUID projectId) {
        return productService.list(projectId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Update quantity or notes")
    public ProjectProductResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectProductRequest request
    ) {
        return productService.update(projectId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Remove a product from the project (does not change inventory)")
    public ResponseEntity<Void> remove(@PathVariable UUID projectId, @PathVariable UUID id) {
        productService.remove(projectId, id);
        return ResponseEntity.noContent().build();
    }
}
