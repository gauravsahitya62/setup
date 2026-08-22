package com.flowset.planner.booking.controller;

import com.flowset.common.BookingRequestStatus;
import com.flowset.planner.booking.dto.BookingRequestResponse;
import com.flowset.planner.booking.dto.BookingRequestSummaryResponse;
import com.flowset.planner.booking.dto.RejectBookingRequest;
import com.flowset.planner.booking.dto.VendorBookingStatsResponse;
import com.flowset.planner.booking.service.BookingRequestService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendor/booking-requests")
@Tag(
        name = "Vendor Booking Requests",
        description = "Vendor inbox. vendorId is taken from the authenticated owner — never from the client."
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('VENDOR')")
public class VendorBookingRequestController {

    private final BookingRequestService bookingRequestService;

    public VendorBookingRequestController(BookingRequestService bookingRequestService) {
        this.bookingRequestService = bookingRequestService;
    }

    @GetMapping
    @Operation(summary = "List this vendor's booking requests")
    public PageResponse<BookingRequestSummaryResponse> list(
            @RequestParam(required = false) BookingRequestStatus status,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return bookingRequestService.list(projectId, status, null, null, page, size);
    }

    @GetMapping("/stats")
    @Operation(summary = "Counts by status for the vendor dashboard")
    public VendorBookingStatsResponse stats() {
        return bookingRequestService.vendorStats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a request belonging to this vendor", description = "Records VENDOR_VIEWED the first time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request"),
            @ApiResponse(responseCode = "404", description = "Not found (also used to hide other vendors' requests)")
    })
    public BookingRequestResponse getById(@PathVariable UUID id) {
        return bookingRequestService.getById(id, true);
    }

    @PostMapping("/{id}/accept")
    @Operation(
            summary = "Accept a pending request",
            description = """
                    Re-checks availability, then in one transaction: PENDING→ACCEPTED, CONFIRMED reservations via
                    InventoryAvailabilityService, ACCEPTED→CONFIRMED. If stock is gone the request stays PENDING.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Confirmed"),
            @ApiResponse(responseCode = "409", description = "Inventory gone or invalid transition", content = @Content(examples = @ExampleObject(value = """
                    { "status": 409, "error": "Conflict", "message": "Inventory is no longer available for the requested dates." }
                    """)))
    })
    public BookingRequestResponse accept(@PathVariable UUID id) {
        return bookingRequestService.accept(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rejected"),
            @ApiResponse(responseCode = "400", description = "Rejection reason required"),
            @ApiResponse(responseCode = "409", description = "Invalid transition")
    })
    public BookingRequestResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectBookingRequest body) {
        return bookingRequestService.reject(id, body);
    }
}
