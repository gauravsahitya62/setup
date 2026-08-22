package com.flowset.planner.projects.dto;

import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
        @Size(max = 200) String title,
        @Size(max = 20000) String content,
        Boolean pinned
) {
}
