package com.flowset.planner.projects.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectFileResponse(
        UUID id,
        UUID projectId,
        String fileUrl,
        String fileName,
        String fileType,
        long fileSize,
        UUID uploadedBy,
        String uploadedByName,
        Instant createdAt
) {
}
