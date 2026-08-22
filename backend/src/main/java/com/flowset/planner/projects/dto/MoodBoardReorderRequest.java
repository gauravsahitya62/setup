package com.flowset.planner.projects.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record MoodBoardReorderRequest(
        @NotEmpty(message = "Item order list is required") List<UUID> itemIds
) {
}
