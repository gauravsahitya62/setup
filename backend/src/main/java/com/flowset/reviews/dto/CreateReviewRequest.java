package com.flowset.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Create a review for a completed order. plannerId and vendorId are taken from the order, never from the client.")
public record CreateReviewRequest(
        @NotNull UUID orderId,
        @NotNull @Min(1) @Max(5) Integer overallRating,
        @Min(1) @Max(5) Integer communicationRating,
        @Min(1) @Max(5) Integer productQualityRating,
        @Min(1) @Max(5) Integer deliveryRating,
        @Min(1) @Max(5) Integer valueRating,
        @Size(max = 2000) String reviewText
) {
}
