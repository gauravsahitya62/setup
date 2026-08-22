package com.flowset.planner.booking.controller;

import com.flowset.common.BookingRequestStatus;
import com.flowset.planner.booking.dto.BookingRequestResponse;
import com.flowset.planner.booking.dto.BookingRequestSummaryResponse;
import com.flowset.planner.booking.dto.CreateBookingRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/booking-requests")
@Tag(
        name = "Booking Requests",
        description = """
                Planner booking requests. Inventory is NOT reserved on create.
                Status transitions: DRAFT→PENDING, PENDING→ACCEPTED|REJECTED|CANCELLED, ACCEPTED→CONFIRMED, CONFIRMED→CANCELLED.
                Vendors accept/reject via /api/v1/vendor/booking-requests.
                """
)
@SecurityRequirement(name = "bearerAuth")
public class PlannerBookingRequestController {

    private final BookingRequestService bookingRequestService;

    public PlannerBookingRequestController(BookingRequestService bookingRequestService) {
        this.bookingRequestService = bookingRequestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(
            summary = "Send a booking request",
            description = """
                    plannerId is taken from the JWT. Availability is checked but not reserved.
                    Duplicate PENDING requests (same planner, project, vendor, products, dates) are rejected.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created", content = @Content(examples = @ExampleObject(value = """
                    { "status": "PENDING", "currency": "INR", "estimatedRentalValue": 5400.00 }
                    """))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not a planner"),
            @ApiResponse(responseCode = "409", description = "Insufficient availability or duplicate pending request", content = @Content(examples = {
                    @ExampleObject(name = "availability", value = """
                            { "status": 409, "error": "Conflict", "message": "Only 70 units are currently available for the selected dates." }
                            """),
                    @ExampleObject(name = "duplicate", value = """
                            { "status": 409, "error": "Conflict", "message": "A pending booking request already exists for this vendor, products, and dates." }
                            """)
            }))
    })
    public ResponseEntity<BookingRequestResponse> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingRequestService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "List booking requests", description = "Planners see their own requests. Admins may filter by plannerId or vendorId.")
    public PageResponse<BookingRequestSummaryResponse> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) BookingRequestStatus status,
            @RequestParam(required = false) UUID plannerId,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return bookingRequestService.list(projectId, status, plannerId, vendorId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLANNER','ADMIN')")
    @Operation(summary = "Get a booking request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request"),
            @ApiResponse(responseCode = "404", description = "Not found (also used to hide other planners' requests)")
    })
    public BookingRequestResponse getById(@PathVariable UUID id) {
        return bookingRequestService.getById(id, false);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(
            summary = "Cancel a booking request",
            description = "Allowed from PENDING or CONFIRMED. CONFIRMED releases inventory via InventoryAvailabilityService. COMPLETED cannot be cancelled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelled"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public BookingRequestResponse cancel(@PathVariable UUID id) {
        return bookingRequestService.cancel(id);
    }
}
