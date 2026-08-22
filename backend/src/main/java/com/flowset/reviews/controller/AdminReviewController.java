package com.flowset.reviews.controller;

import com.flowset.common.ReviewStatus;
import com.flowset.reviews.dto.ModerateReviewRequest;
import com.flowset.reviews.dto.ReviewModerationActionResponse;
import com.flowset.reviews.dto.ReviewReportResponse;
import com.flowset.reviews.dto.ReviewResponse;
import com.flowset.reviews.service.ReviewService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@Tag(name = "Admin Reviews", description = "Moderation. Hide removes a review from public aggregates; restore publishes it again. Every action is audited.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "List all reviews")
    public PageResponse<ReviewResponse> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) UUID plannerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.adminList(status, vendorId, plannerId, page, size);
    }

    @GetMapping("/reported")
    @Operation(summary = "Reviews with open reports")
    public PageResponse<ReviewResponse> reported(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.adminReported(page, size);
    }

    @PatchMapping("/{id}/moderate")
    @Operation(
            summary = "Moderate a review",
            description = "HIDE, RESTORE, REMOVE, or NOTE. REMOVE is a soft delete. Recalculates vendor rating except for NOTE."
    )
    public ReviewResponse moderate(@PathVariable UUID id, @Valid @RequestBody ModerateReviewRequest request) {
        return reviewService.moderate(id, request);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Moderation history")
    public List<ReviewModerationActionResponse> history(@PathVariable UUID id) {
        return reviewService.history(id);
    }

    @GetMapping("/{id}/reports")
    @Operation(summary = "Reports filed against a review")
    public List<ReviewReportResponse> reports(@PathVariable UUID id) {
        return reviewService.reports(id);
    }
}
