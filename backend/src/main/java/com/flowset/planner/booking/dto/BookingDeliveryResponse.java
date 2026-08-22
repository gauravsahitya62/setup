package com.flowset.planner.booking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingDeliveryResponse(
        LocalDate deliveryDate,
        LocalTime preferredDeliveryTime,
        String venueName,
        String venueAddress,
        String city,
        String state,
        String contactPerson,
        String contactPhone,
        String deliveryNotes,
        LocalDate pickupDate,
        LocalTime preferredPickupTime
) {
}
