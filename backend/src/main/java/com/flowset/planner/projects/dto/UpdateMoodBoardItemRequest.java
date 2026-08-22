package com.flowset.planner.projects.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateMoodBoardItemRequest(
        @Size(max = 1000) String sourceUrl,
        @Size(max = 500) String caption,
        @Min(0) Integer displayOrder
) {
}
