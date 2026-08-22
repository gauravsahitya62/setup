package com.flowset.reviews.dto;

import com.flowset.common.ReviewModerationActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin moderation. HIDE / RESTORE / REMOVE change visibility; NOTE records a comment only.")
public record ModerateReviewRequest(
        @NotNull ReviewModerationActionType action,
        @Size(max = 2000) String note
) {
}
