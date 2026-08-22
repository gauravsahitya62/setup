package com.flowset.planner.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowset.planner.booking.service.BookingPricing;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookingPricingTest {

    @Test
    void inclusiveDaysAndLineValue() {
        assertThat(BookingPricing.rentalDays(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 14))).isEqualTo(3);
        assertThat(BookingPricing.lineValue(new BigDecimal("18.00"), 100, 3)).isEqualByComparingTo("5400.00");
    }
}
