package com.flowset.planner.projects.controller;

import com.flowset.planner.projects.dto.ProjectFileResponse;
import com.flowset.planner.projects.service.ProjectFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/files")
@Tag(name = "Project Files", description = "Metadata in PostgreSQL; bytes via StorageService")
@SecurityRequirement(name = "bearerAuth")
public class ProjectFileController {

    private final ProjectFileService fileService;

    public ProjectFileController(ProjectFileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Upload a project file")
    public ResponseEntity<ProjectFileResponse> upload(
            @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.upload(projectId, file));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List project files")
    public List<ProjectFileResponse> list(@PathVariable UUID projectId) {
        return fileService.list(projectId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Delete a project file")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID id) {
        fileService.delete(projectId, id);
        return ResponseEntity.noContent().build();
    }
}
