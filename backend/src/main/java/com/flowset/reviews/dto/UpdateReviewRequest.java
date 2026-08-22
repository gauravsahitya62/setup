package com.flowset.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Edit a planner's own review within the edit window.")
public record UpdateReviewRequest(
        @Min(1) @Max(5) Integer overallRating,
        @Min(1) @Max(5) Integer communicationRating,
        @Min(1) @Max(5) Integer productQualityRating,
        @Min(1) @Max(5) Integer deliveryRating,
        @Min(1) @Max(5) Integer valueRating,
        @Size(max = 2000) String reviewText
) {
}
