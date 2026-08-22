package com.flowset.planner.booking.dto;

import com.flowset.common.BookingRejectionReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectBookingRequest(
        @NotNull(message = "Rejection reason is required") BookingRejectionReason reason,
        @Size(max = 2000) String note
) {
}
