package com.flowset.orders.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record DeliveryUpdateRequest(
        LocalDate deliveryDate,
        LocalTime preferredTime,
        @Size(max = 255) String venueName,
        @Size(max = 500) String venueAddress,
        @Size(max = 120) String city,
        @Size(max = 120) String state,
        @Size(max = 200) String contactPerson,
        @Size(max = 40) String contactPhone,
        @Size(max = 4000) String deliveryNotes,
        BigDecimal deliveryFee,
        BigDecimal securityDeposit
) {
}
