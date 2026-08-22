package com.flowset.reviews.controller;

import com.flowset.common.ReviewStatus;
import com.flowset.reviews.dto.CreateReviewRequest;
import com.flowset.reviews.dto.DeleteReviewRequest;
import com.flowset.reviews.dto.EligibleReviewResponse;
import com.flowset.reviews.dto.ReportReviewRequest;
import com.flowset.reviews.dto.ReviewReportResponse;
import com.flowset.reviews.dto.ReviewResponse;
import com.flowset.reviews.dto.UpdateReviewRequest;
import com.flowset.reviews.service.ReviewEligibilityService;
import com.flowset.reviews.service.ReviewService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Planner reviews of completed orders. Identity is taken from the JWT — never from plannerId/vendorId in the body.")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewEligibilityService eligibilityService;

    public ReviewController(ReviewService reviewService, ReviewEligibilityService eligibilityService) {
        this.reviewService = reviewService;
        this.eligibilityService = eligibilityService;
    }

    @GetMapping("/eligible")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "List completed orders the planner can still review")
    public List<EligibleReviewResponse> eligible() {
        return eligibilityService.listEligible();
    }

    @GetMapping("/eligible/{orderId}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Eligibility for a specific order, including expired windows")
    public EligibleReviewResponse eligibleForOrder(@PathVariable UUID orderId) {
        return eligibilityService.eligibilityForOrder(orderId);
    }

    @GetMapping
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "List the authenticated planner's reviews")
    public PageResponse<ReviewResponse> listMine(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.listMine(status, orderId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a review",
            description = "Planner: own review. Vendor: published review about their storefront. Admin: any. Other callers receive 404.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Review"),
                @ApiResponse(responseCode = "404", description = "Missing or not owned")
            }
    )
    public ReviewResponse getById(@PathVariable UUID id) {
        return reviewService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('PLANNER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit a review for a completed order",
            description = "Creates a PUBLISHED verified review. One review per order. Overall rating 1–5 required. "
                    + "plannerId and vendorId are copied from the order.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Published review",
                        content = @Content(examples = @ExampleObject(value = """
                                {
                                  "orderId": "22222222-2222-2222-2222-222222222222",
                                  "overallRating": 5,
                                  "communicationRating": 5,
                                  "productQualityRating": 4,
                                  "deliveryRating": 5,
                                  "valueRating": 5,
                                  "reviewText": "Excellent chairs and on-time delivery."
                                }
                                """))),
                @ApiResponse(responseCode = "400", description = "Validation error (rating out of range, text too long)"),
                @ApiResponse(responseCode = "409", description = "Order not completed, period expired, or duplicate review")
            }
    )
    public ReviewResponse create(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(
            summary = "Edit own review",
            description = "Allowed for flowset.reviews.edit-window-days (default 30) after creation. After that the review is read-only.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Updated"),
                @ApiResponse(responseCode = "404", description = "Not owned"),
                @ApiResponse(responseCode = "409", description = "Edit window expired")
            }
    )
    public ReviewResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(
            summary = "Soft-delete own review",
            description = "Sets deleted_at / deleted_by / delete_reason. UNIQUE(order_id) still prevents a replacement review."
    )
    public ReviewResponse delete(@PathVariable UUID id, @Valid @RequestBody DeleteReviewRequest request) {
        return reviewService.delete(id, request);
    }

    @PostMapping("/{id}/report")
    @Operation(
            summary = "Report a review",
            description = "Authenticated users except the author. Does not hide the review until an admin moderates.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Report recorded"),
                @ApiResponse(responseCode = "409", description = "Duplicate open report")
            }
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewReportResponse report(@PathVariable UUID id, @Valid @RequestBody ReportReviewRequest request) {
        return reviewService.report(id, request);
    }
}
