package com.flowset.planner.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "Project is required") UUID projectId,
        @NotNull(message = "Vendor is required") UUID vendorId,
        @NotEmpty(message = "Select at least one product") @Valid List<Item> items,
        LocalDate eventDate,
        @NotNull(message = "Rental start date is required") LocalDate rentalStartDate,
        @NotNull(message = "Rental end date is required") LocalDate rentalEndDate,
        @Size(max = 255) String venueName,
        @Size(max = 500) String venueAddress,
        @Size(max = 120) String city,
        @Size(max = 120) String state,
        @Size(max = 4000) String plannerNotes,
        @Valid Delivery delivery
) {
    public record Item(
            @NotNull(message = "Variant is required") UUID variantId,
            @NotNull(message = "Quantity is required")
                    @Min(value = 1, message = "Quantity must be at least 1")
                    Integer quantity,
            @Size(max = 2000) String notes
    ) {
    }

    public record Delivery(
            LocalDate deliveryDate,
            LocalTime preferredDeliveryTime,
            @Size(max = 255) String venueName,
            @Size(max = 500) String venueAddress,
            @Size(max = 120) String city,
            @Size(max = 120) String state,
            @Size(max = 200) String contactPerson,
            @Size(max = 40) String contactPhone,
            @Size(max = 4000) String deliveryNotes,
            LocalDate pickupDate,
            LocalTime preferredPickupTime
    ) {
    }
}
