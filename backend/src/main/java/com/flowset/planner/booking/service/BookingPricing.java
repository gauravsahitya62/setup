package com.flowset.planner.booking.service;

import com.flowset.marketplace.inventory.engine.DateRange;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class BookingPricing {

    private BookingPricing() {
    }

    public static int rentalDays(LocalDate start, LocalDate end) {
        return DateRange.inclusiveDays(start, end);
    }

    public static BigDecimal lineValue(BigDecimal pricePerDay, int quantity, int rentalDays) {
        if (pricePerDay == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return pricePerDay
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(rentalDays))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
