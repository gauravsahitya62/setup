package com.flowset.planner.projects.dto;

import com.flowset.common.ProjectActivityType;
import java.time.Instant;
import java.util.UUID;

public record ProjectActivityResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        ProjectActivityType activityType,
        String description,
        String metadata,
        Instant createdAt
) {
}
