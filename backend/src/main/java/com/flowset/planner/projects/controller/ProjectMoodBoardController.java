package com.flowset.planner.projects.controller;

import com.flowset.planner.projects.dto.AddMoodBoardItemRequest;
import com.flowset.planner.projects.dto.MoodBoardItemResponse;
import com.flowset.planner.projects.dto.MoodBoardReorderRequest;
import com.flowset.planner.projects.dto.MoodBoardResponse;
import com.flowset.planner.projects.dto.UpdateMoodBoardItemRequest;
import com.flowset.planner.projects.service.ProjectMoodBoardService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/projects/{projectId}/mood-board")
@Tag(name = "Project Mood Board")
@SecurityRequirement(name = "bearerAuth")
public class ProjectMoodBoardController {

    private final ProjectMoodBoardService moodBoardService;

    public ProjectMoodBoardController(ProjectMoodBoardService moodBoardService) {
        this.moodBoardService = moodBoardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "Get the project mood board")
    public MoodBoardResponse get(@PathVariable UUID projectId) {
        return moodBoardService.get(projectId);
    }

    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Upload a mood board image")
    public ResponseEntity<MoodBoardItemResponse> upload(
            @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String sourceUrl
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moodBoardService.addUploaded(projectId, file, caption, sourceUrl));
    }

    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Add a mood board image by URL")
    public ResponseEntity<MoodBoardItemResponse> addUrl(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddMoodBoardItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moodBoardService.addUrl(projectId, request));
    }

    @PatchMapping("/items/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Update caption, source URL, or display order")
    public MoodBoardItemResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMoodBoardItemRequest request
    ) {
        return moodBoardService.update(projectId, id, request);
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Reorder mood board images")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID projectId,
            @Valid @RequestBody MoodBoardReorderRequest request
    ) {
        moodBoardService.reorder(projectId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Delete a mood board image")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID id) {
        moodBoardService.delete(projectId, id);
        return ResponseEntity.noContent().build();
    }
}
