package com.flowset.orders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record SchedulePickupRequest(
        @NotNull(message = "Pickup date is required") LocalDate pickupDate,
        LocalTime preferredTime,
        @Size(max = 4000) String notes
) {
}
