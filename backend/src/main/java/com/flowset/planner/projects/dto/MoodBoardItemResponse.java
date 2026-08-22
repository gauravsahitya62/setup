package com.flowset.planner.projects.dto;

import java.time.Instant;
import java.util.UUID;

public record MoodBoardItemResponse(
        UUID id,
        UUID moodBoardId,
        String imageUrl,
        String sourceUrl,
        String caption,
        int displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
