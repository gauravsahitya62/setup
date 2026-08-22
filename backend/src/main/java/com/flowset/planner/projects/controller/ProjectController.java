package com.flowset.planner.projects.controller;

import com.flowset.common.EventType;
import com.flowset.common.ProjectStatus;
import com.flowset.planner.projects.dto.CreateProjectRequest;
import com.flowset.planner.projects.dto.ProjectResponse;
import com.flowset.planner.projects.dto.ProjectSummaryResponse;
import com.flowset.planner.projects.dto.UpdateProjectRequest;
import com.flowset.planner.projects.service.ProjectService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Planner event workspaces. Creating a project does not reserve inventory.")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(
            summary = "Create a project",
            description = "plannerId is taken from the authenticated user. Never sent by the client. Default status is DRAFT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(examples = @ExampleObject(value = """
                    { "name": "Sharma Wedding", "eventType": "WEDDING", "status": "DRAFT", "progress": { "percent": 10, "completed": 1, "total": 10 } }
                    """))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(examples = @ExampleObject(value = """
                    { "status": 400, "error": "Validation Failed", "message": "Name is required" }
                    """))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not a planner")
    })
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List projects", description = "Planners see their own projects. Admins may filter by plannerId for support.")
    public PageResponse<ProjectSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) UUID plannerId,
            @RequestParam(defaultValue = "eventDate") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return projectService.list(q, status, eventType, plannerId, sort, direction, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a project by id", description = "Returns 404 for missing projects and for projects the caller does not own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project"),
            @ApiResponse(responseCode = "404", description = "Not found (also used to hide other planners' projects)")
    })
    public ProjectResponse getById(@PathVariable UUID id) {
        return projectService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Get a project by slug")
    public ProjectResponse getBySlug(@PathVariable String slug) {
        return projectService.getBySlug(slug);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Update a project")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Soft-delete a project")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Upload project cover image")
    public ProjectResponse uploadCover(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return projectService.uploadCover(id, file);
    }
}
