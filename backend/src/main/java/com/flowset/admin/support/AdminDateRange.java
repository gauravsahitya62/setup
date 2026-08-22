package com.flowset.admin.support;

import com.flowset.exceptions.BadRequestException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class AdminDateRange {

    private AdminDateRange() {
    }

    public static Instant from(String range, Instant customFrom, Instant customTo) {
        String normalized = range == null || range.isBlank() ? "DAYS_30" : range.trim().toUpperCase();
        Instant now = Instant.now();
        return switch (normalized) {
            case "TODAY" -> LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            case "DAYS_7", "7", "7D" -> now.minus(7, ChronoUnit.DAYS);
            case "DAYS_30", "30", "30D" -> now.minus(30, ChronoUnit.DAYS);
            case "DAYS_90", "90", "90D" -> now.minus(90, ChronoUnit.DAYS);
            case "CUSTOM" -> {
                if (customFrom == null) {
                    throw new BadRequestException("from is required for a custom date range");
                }
                if (customTo != null && customTo.isBefore(customFrom)) {
                    throw new BadRequestException("to must be after from");
                }
                yield customFrom;
            }
            default -> throw new BadRequestException("Unknown range. Use TODAY, DAYS_7, DAYS_30, DAYS_90, or CUSTOM");
        };
    }

    public static Instant to(String range, Instant customTo) {
        String normalized = range == null || range.isBlank() ? "DAYS_30" : range.trim().toUpperCase();
        if ("CUSTOM".equals(normalized)) {
            return customTo == null ? Instant.now() : customTo;
        }
        return Instant.now();
    }
}
