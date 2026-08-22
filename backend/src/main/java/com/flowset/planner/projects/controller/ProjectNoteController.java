package com.flowset.planner.projects.controller;

import com.flowset.planner.projects.dto.CreateNoteRequest;
import com.flowset.planner.projects.dto.ProjectNoteResponse;
import com.flowset.planner.projects.dto.UpdateNoteRequest;
import com.flowset.planner.projects.service.ProjectNoteService;
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
@RequestMapping("/api/v1/projects/{projectId}/notes")
@Tag(name = "Project Notes")
@SecurityRequirement(name = "bearerAuth")
public class ProjectNoteController {

    private final ProjectNoteService noteService;

    public ProjectNoteController(ProjectNoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Create a note")
    public ResponseEntity<ProjectNoteResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateNoteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(projectId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List notes")
    public List<ProjectNoteResponse> list(@PathVariable UUID projectId) {
        return noteService.list(projectId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Update a note (including pin)")
    public ProjectNoteResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        return noteService.update(projectId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Delete a note")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID id) {
        noteService.delete(projectId, id);
        return ResponseEntity.noContent().build();
    }
}
