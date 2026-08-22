package com.flowset.planner.projects.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectNoteResponse(
        UUID id,
        UUID projectId,
        String title,
        String content,
        boolean pinned,
        UUID createdBy,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
}
