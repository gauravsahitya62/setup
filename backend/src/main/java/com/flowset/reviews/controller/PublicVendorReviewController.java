package com.flowset.reviews.controller;

import com.flowset.common.ReviewSort;
import com.flowset.reviews.dto.VendorReviewPageResponse;
import com.flowset.reviews.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/reviews")
@Tag(name = "Vendor Reviews", description = "Public published reviews for a vendor. Aggregates use PUBLISHED rows only.")
public class PublicVendorReviewController {

    private final ReviewService reviewService;

    public PublicVendorReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(
            summary = "List published vendor reviews",
            description = "Paginated. Sort: NEWEST (default), HIGHEST, LOWEST. Filter by overall rating (1–5) and verified. "
                    + "Planner last names and emails are never returned. Summary.averageRating is NUMERIC(3,2) from integer stars.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Page of published reviews plus rating summary",
                        content = @Content(examples = @ExampleObject(value = """
                                {
                                  "content": [{
                                    "id": "11111111-1111-1111-1111-111111111111",
                                    "plannerDisplayName": "Maya",
                                    "plannerInitials": "MC",
                                    "overallRating": 5,
                                    "reviewText": "Excellent chairs and on-time delivery.",
                                    "verified": true
                                  }],
                                  "page": 0,
                                  "size": 12,
                                  "totalElements": 1,
                                  "totalPages": 1,
                                  "summary": {
                                    "averageRating": 5.00,
                                    "reviewCount": 1,
                                    "verifiedCount": 1,
                                    "distribution": { "5": 1, "4": 0, "3": 0, "2": 0, "1": 0 }
                                  }
                                }
                                """))),
                @ApiResponse(responseCode = "404", description = "Vendor not found")
            }
    )
    public VendorReviewPageResponse list(
            @PathVariable UUID vendorId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) ReviewSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return reviewService.listPublic(vendorId, rating, verified, sort, page, size);
    }
}
