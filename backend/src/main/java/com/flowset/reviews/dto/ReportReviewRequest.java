package com.flowset.reviews.dto;

import com.flowset.common.ReviewReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Report a review. Review text is never echoed back through notifications.")
public record ReportReviewRequest(
        @NotNull ReviewReportReason reason,
        @Size(max = 2000) String description
) {
}
