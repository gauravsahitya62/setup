package com.flowset.marketplace.inventory.engine;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inclusive rental-date occupancy. Two ranges overlap when
 * {@code a.start <= b.end && a.end >= b.start}. Adjacent ranges
 * (A ends on day D, B starts on D+1) do not overlap.
 *
 * Peak occupancy is computed with a sweep line so overlapping and
 * non-overlapping intervals are not double-counted across a window.
 */
public final class OccupancyCalculator {

    public record Interval(LocalDate start, LocalDate end, int quantity) {
        public Interval {
            if (start == null || end == null) {
                throw new IllegalArgumentException("Interval dates are required");
            }
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("Interval end must be on or after start");
            }
        }

        public boolean overlaps(LocalDate rangeStart, LocalDate rangeEnd) {
            return DateRange.overlaps(start, end, rangeStart, rangeEnd);
        }

        public boolean covers(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }
    }

    private OccupancyCalculator() {
    }

    /**
     * Highest quantity occupied on any single day inside {@code [rangeStart, rangeEnd]}.
     * Intervals that do not overlap the window are ignored.
     */
    public static int peakOccupied(List<Interval> intervals, LocalDate rangeStart, LocalDate rangeEnd) {
        DateRange.requireValid(rangeStart, rangeEnd);
        if (intervals == null || intervals.isEmpty()) {
            return 0;
        }

        List<int[]> events = new ArrayList<>();
        for (Interval interval : intervals) {
            if (interval.quantity() == 0 || !interval.overlaps(rangeStart, rangeEnd)) {
                continue;
            }
            LocalDate clippedStart = interval.start().isBefore(rangeStart) ? rangeStart : interval.start();
            LocalDate clippedEnd = interval.end().isAfter(rangeEnd) ? rangeEnd : interval.end();
            events.add(new int[] {epochDay(clippedStart), interval.quantity()});
            events.add(new int[] {epochDay(clippedEnd) + 1, -interval.quantity()});
        }
        if (events.isEmpty()) {
            return 0;
        }

        events.sort(Comparator.<int[]>comparingInt(e -> e[0]).thenComparingInt(e -> e[1]));
        int running = 0;
        int peak = 0;
        for (int[] event : events) {
            running += event[1];
            if (running > peak) {
                peak = running;
            }
        }
        return peak;
    }

    public static int occupiedOn(List<Interval> intervals, LocalDate date) {
        if (intervals == null || intervals.isEmpty() || date == null) {
            return 0;
        }
        int total = 0;
        for (Interval interval : intervals) {
            if (interval.covers(date)) {
                total += interval.quantity();
            }
        }
        return total;
    }

    public static List<LocalDate> daysInclusive(LocalDate start, LocalDate end) {
        DateRange.requireValid(start, end);
        long count = ChronoUnit.DAYS.between(start, end) + 1;
        List<LocalDate> days = new ArrayList<>((int) count);
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            days.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    private static int epochDay(LocalDate date) {
        return (int) date.toEpochDay();
    }
}
