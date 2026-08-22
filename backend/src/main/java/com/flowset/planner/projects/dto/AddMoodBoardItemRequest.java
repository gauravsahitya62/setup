package com.flowset.planner.projects.dto;

import jakarta.validation.constraints.Size;

public record AddMoodBoardItemRequest(
        @Size(max = 1000) String imageUrl,
        @Size(max = 1000) String sourceUrl,
        @Size(max = 500) String caption
) {
}
