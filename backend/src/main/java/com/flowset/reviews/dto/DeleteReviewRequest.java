package com.flowset.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Soft-delete the planner's own review.")
public record DeleteReviewRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
