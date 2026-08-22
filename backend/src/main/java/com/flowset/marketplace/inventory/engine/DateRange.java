package com.flowset.marketplace.inventory.engine;

import com.flowset.exceptions.BadRequestException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Inclusive rental dates. {@code 12 Dec – 14 Dec} covers the 12th, 13th and 14th.
 */
public final class DateRange {

    private DateRange() {
    }

    public static boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !aStart.isAfter(bEnd) && !aEnd.isBefore(bStart);
    }

    public static void requireValid(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("End date must be on or after start date");
        }
    }

    /** Inclusive day count: 12 Dec–14 Dec is 3 rental days. */
    public static int inclusiveDays(LocalDate start, LocalDate end) {
        requireValid(start, end);
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }
}
