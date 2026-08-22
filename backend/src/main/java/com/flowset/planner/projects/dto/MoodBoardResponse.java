package com.flowset.planner.projects.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MoodBoardResponse(
        UUID id,
        UUID projectId,
        List<MoodBoardItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
