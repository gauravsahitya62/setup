package com.flowset.marketplace.inventory.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowset.exceptions.BadRequestException;
import com.flowset.marketplace.inventory.engine.OccupancyCalculator.Interval;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OccupancyCalculatorTest {

    private static final LocalDate DEC_10 = LocalDate.of(2026, 12, 10);
    private static final LocalDate DEC_11 = LocalDate.of(2026, 12, 11);
    private static final LocalDate DEC_12 = LocalDate.of(2026, 12, 12);
    private static final LocalDate DEC_13 = LocalDate.of(2026, 12, 13);
    private static final LocalDate DEC_14 = LocalDate.of(2026, 12, 14);
    private static final LocalDate DEC_15 = LocalDate.of(2026, 12, 15);

    @Test
    void noReservationsYieldsZeroOccupancy() {
        assertThat(OccupancyCalculator.peakOccupied(List.of(), DEC_12, DEC_13)).isZero();
    }

    @Test
    void overlappingRangesAreDetected() {
        assertThat(DateRange.overlaps(DEC_10, DEC_12, DEC_11, DEC_13)).isTrue();
    }

    @Test
    void adjacentRangesDoNotOverlap() {
        assertThat(DateRange.overlaps(DEC_10, DEC_12, DEC_13, DEC_15)).isFalse();
    }

    @Test
    void sameDayBookingCoversThatDayOnly() {
        Interval sameDay = new Interval(DEC_12, DEC_12, 40);
        assertThat(OccupancyCalculator.peakOccupied(List.of(sameDay), DEC_12, DEC_12)).isEqualTo(40);
        assertThat(OccupancyCalculator.occupiedOn(List.of(sameDay), DEC_13)).isZero();
    }

    @Test
    void multiDayBookingIsInclusive() {
        Interval multi = new Interval(DEC_12, DEC_14, 30);
        assertThat(OccupancyCalculator.daysInclusive(DEC_12, DEC_14))
                .containsExactly(DEC_12, DEC_13, DEC_14);
        assertThat(OccupancyCalculator.occupiedOn(List.of(multi), DEC_12)).isEqualTo(30);
        assertThat(OccupancyCalculator.occupiedOn(List.of(multi), DEC_14)).isEqualTo(30);
    }

    @Test
    void overlappingBookingsPeakOnSharedDays() {
        Interval a = new Interval(DEC_10, DEC_12, 40);
        Interval b = new Interval(DEC_11, DEC_13, 30);
        assertThat(OccupancyCalculator.peakOccupied(List.of(a, b), DEC_10, DEC_13)).isEqualTo(70);
        assertThat(OccupancyCalculator.occupiedOn(List.of(a, b), DEC_10)).isEqualTo(40);
        assertThat(OccupancyCalculator.occupiedOn(List.of(a, b), DEC_11)).isEqualTo(70);
        assertThat(OccupancyCalculator.occupiedOn(List.of(a, b), DEC_13)).isEqualTo(30);
    }

    @Test
    void nonOverlappingBookingsDoNotStackAcrossTheWindow() {
        Interval a = new Interval(DEC_12, DEC_13, 40);
        Interval b = new Interval(DEC_15, DEC_15.plusDays(1), 30);
        assertThat(OccupancyCalculator.peakOccupied(List.of(a, b), DEC_12, DEC_15.plusDays(1))).isEqualTo(40);
    }

    @Test
    void adjacentBookingsDoNotStack() {
        Interval a = new Interval(DEC_10, DEC_12, 40);
        Interval c = new Interval(DEC_13, DEC_15, 30);
        assertThat(OccupancyCalculator.peakOccupied(List.of(a, c), DEC_10, DEC_15)).isEqualTo(40);
        assertThat(OccupancyCalculator.occupiedOn(List.of(a, c), DEC_12)).isEqualTo(40);
        assertThat(OccupancyCalculator.occupiedOn(List.of(a, c), DEC_13)).isEqualTo(30);
    }

    @Test
    void cancelledStyleIntervalsAreSimplyOmittedByCaller() {
        Interval confirmed = new Interval(DEC_12, DEC_13, 40);
        assertThat(OccupancyCalculator.peakOccupied(List.of(confirmed), DEC_12, DEC_13)).isEqualTo(40);
        assertThat(OccupancyCalculator.peakOccupied(List.of(), DEC_12, DEC_13)).isZero();
    }

    @Test
    void invalidDateRangeIsRejected() {
        assertThatThrownBy(() -> DateRange.requireValid(DEC_14, DEC_12))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End date");
    }
}
